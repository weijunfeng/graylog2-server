/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.graylog2.alerts.types;

import com.google.common.collect.Lists;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.Configuration;
import org.graylog2.alerts.AbstractAlertCondition;
import org.graylog2.indexer.InvalidRangeFormatException;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.indexer.results.SearchResult;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.indexer.searches.Sorting;
import org.graylog2.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageSummary;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.streams.Stream;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;

public class FieldContentValueAlertCondition extends AbstractAlertCondition {
    private static final Logger LOG = LoggerFactory.getLogger(FieldContentValueAlertCondition.class);

    private final Searches searches;
    private final Configuration configuration;
    private final String field;
    private final String value;

    public interface Factory {
        FieldContentValueAlertCondition createAlertCondition(Stream stream, String id, DateTime createdAt, @Assisted("userid") String creatorUserId, Map<String, Object> parameters);
    }

    @AssistedInject
    public FieldContentValueAlertCondition(Searches searches, Configuration configuration, @Assisted Stream stream, @Nullable @Assisted String id, @Assisted DateTime createdAt, @Assisted("userid") String creatorUserId, @Assisted Map<String, Object> parameters) {
        super(stream, id, Type.FIELD_CONTENT_VALUE, createdAt, creatorUserId, parameters);
        this.searches = searches;
        this.configuration = configuration;
        this.field = (String) parameters.get("field");
        this.value = (String) parameters.get("value");

        checkArgument(!isNullOrEmpty(field), "\"field\" must not be empty.");
        checkArgument(!isNullOrEmpty(value), "\"value\" must not be empty.");
    }

    @Override
    protected CheckResult runCheck() {
        String filter = "streams:" + stream.getId();
        String query = field + ":\"" + value + "\"";
        Integer backlogSize = getBacklog();
        boolean backlogEnabled = false;
        int searchLimit = 1;

        if(backlogSize != null && backlogSize > 0) {
            backlogEnabled = true;
            searchLimit = backlogSize;
        }

        try {
            SearchResult result = searches.search(
                    query,
                    filter,
                    RelativeRange.create(configuration.getAlertCheckInterval()),
                    searchLimit,
                    0,
                    new Sorting("timestamp", Sorting.Direction.DESC)
            );

            final List<MessageSummary> summaries;
            if (backlogEnabled) {
                summaries = Lists.newArrayListWithCapacity(result.getResults().size());
                for (ResultMessage resultMessage : result.getResults()) {
                    final Message msg = resultMessage.getMessage();
                    summaries.add(new MessageSummary(resultMessage.getIndex(), msg));
                }
            } else {
                summaries = Collections.emptyList();
            }

            final long count = result.getTotalResults();

            final String resultDescription = "Stream received messages matching <" + query + "> "
                    + "(Current grace time: " + grace + " minutes)";

            if (count > 0) {
                LOG.debug("Alert check <{}> found [{}] messages.", id, count);
                return new CheckResult(true, this, resultDescription, Tools.iso8601(), summaries);
            } else {
                LOG.debug("Alert check <{}> returned no results.", id);
                return new NegativeCheckResult(this);
            }
        } catch (InvalidRangeParametersException e) {
            // cannot happen lol
            LOG.error("Invalid timerange.", e);
            return null;
        } catch (InvalidRangeFormatException e) {
            // lol same here
            LOG.error("Invalid timerange format.", e);
            return null;
        }
    }

    @Override
    public String getDescription() {
        return "field: " + field + ", value: " + value;
    }
}
