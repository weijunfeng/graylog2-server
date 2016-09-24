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
package org.graylog2.indexer;

import com.google.common.collect.Lists;
import org.graylog2.indexer.indices.TooManyAliasesException;

import javax.inject.Inject;
import java.util.List;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

public class LegacyDeflectorRegistry implements IndexSetRegistry {
    private final IndexSet indexSet;
    private final List<IndexSet> indexSetRegistry;

    @Inject
    public LegacyDeflectorRegistry(IndexSet indexSet) {
        this.indexSet = requireNonNull(indexSet);
        this.indexSetRegistry = Lists.newArrayList(indexSet);
    }

    @Override
    public List<IndexSet> getAllIndexSets() {
        return indexSetRegistry;
    }

    @Override
    public void forEach(Consumer<IndexSet> action) {
        requireNonNull(action);
        indexSetRegistry.forEach(action);
    }

    @Override
    public String[] getAllGraylogIndexNames() {
        return indexSet.getAllGraylogIndexNames();
    }

    @Override
    public boolean isGraylogIndex(String indexName) {
        return indexSet.isGraylogIndex(indexName);
    }

    @Override
    public String[] getWriteIndexWildcards() {
        return new String[]{indexSet.getWriteIndexWildcard()};
    }

    @Override
    public String[] getWriteIndexNames() {
        return new String[]{indexSet.getWriteIndexAlias()};
    }

    @Override
    public boolean isUp() {
        // TODO 2.2: Replace with a real implementation or check if we can get rid of it.
        return indexSet.isUp();
    }

    @Override
    public boolean isCurrentWriteIndexAlias(String indexName) {
        return indexSet.isDeflectorAlias(indexName);
    }

    @Override
    public boolean isCurrentWriteIndex(String indexName) throws TooManyAliasesException {
        final String currentWriteIndex = indexSet.getCurrentActualTargetIndex();

        return currentWriteIndex != null && currentWriteIndex.equals(indexName);
    }
}