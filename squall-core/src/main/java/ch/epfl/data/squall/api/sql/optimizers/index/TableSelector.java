/*
 * Copyright (c) 2011-2015 EPFL DATA Laboratory
 * Copyright (c) 2014-2015 The Squall Collaboration (see NOTICE)
 *
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.epfl.data.squall.api.sql.optimizers.index;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.sf.jsqlparser.schema.Table;
import ch.epfl.data.squall.api.sql.schema.Schema;
import ch.epfl.data.squall.api.sql.util.ParserUtil;
import ch.epfl.data.squall.api.sql.util.TableAliasName;

public class TableSelector {
    public class PairTableNameSize implements Comparable<PairTableNameSize> {
	private final String _tableName;
	private final long _size;

	public PairTableNameSize(Table table, long size) {
	    _tableName = ParserUtil.getComponentName(table);
	    _size = size;
	}

	@Override
	public int compareTo(PairTableNameSize t) {
	    final long otherSize = t.getSize();
	    return (new Long(_size)).compareTo(new Long(otherSize));
	}

	public long getSize() {
	    return _size;
	}

	public String getTableName() {
	    return _tableName;
	}
    }

    private final List<PairTableNameSize> _pairsTableNameSize;

    private final TableAliasName _tan;

    public TableSelector(List<Table> listTables, Schema schema,
	    TableAliasName tan) {
	_tan = tan;

	// generateSubplan (table, size) list from tables from the query
	_pairsTableNameSize = createSizePairs(listTables, schema);
	// in place sort
	Collections.sort(_pairsTableNameSize);
    }

    private List<PairTableNameSize> createSizePairs(List<Table> listTables,
	    Schema schema) {
	final List<PairTableNameSize> pairsTableSize = new ArrayList<PairTableNameSize>();
	for (final Table table : listTables) {
	    final String schemaName = _tan.getSchemaName(ParserUtil
		    .getComponentName(table));
	    final long tableSize = schema.getTableSize(schemaName);

	    final PairTableNameSize pts = new PairTableNameSize(table,
		    tableSize);
	    pairsTableSize.add(pts);
	}
	return pairsTableSize;
    }

    public List<String> removeAll() {
	final List<String> tableNameList = new ArrayList<String>();
	while (!_pairsTableNameSize.isEmpty())
	    tableNameList.add(_pairsTableNameSize.remove(0).getTableName());
	return tableNameList;
    }

    // Best means smallest available from the pairs
    public String removeBestPairedTableName(List<String> joinedWith) {
	for (int i = 0; i < _pairsTableNameSize.size(); i++) {
	    final PairTableNameSize pts = _pairsTableNameSize.get(i);
	    final String currentTableName = pts.getTableName();
	    if (joinedWith.contains(currentTableName)) {
		_pairsTableNameSize.remove(i);
		return currentTableName;
	    }
	}
	// all the pairs I can join with are already taken
	return null;
    }

    // best means the smallest available
    public String removeBestTableName() {
	return _pairsTableNameSize.remove(0).getTableName();
    }

    public int size() {
	return _pairsTableNameSize.size();
    }

}
