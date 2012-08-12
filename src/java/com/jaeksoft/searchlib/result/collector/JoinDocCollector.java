/**   
 * License Agreement for OpenSearchServer
 *
 * Copyright (C) 2012 Emmanuel Keller / Jaeksoft
 * 
 * http://www.open-search-server.com
 * 
 * This file is part of OpenSearchServer.
 *
 * OpenSearchServer is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * OpenSearchServer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with OpenSearchServer. 
 *  If not, see <http://www.gnu.org/licenses/>.
 **/

package com.jaeksoft.searchlib.result.collector;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.lucene.search.FieldCache.StringIndex;
import org.apache.lucene.util.OpenBitSet;

import com.jaeksoft.searchlib.sort.AscStringIndexSorter;
import com.jaeksoft.searchlib.util.StringUtils;
import com.jaeksoft.searchlib.util.Timer;

public class JoinDocCollector implements JoinDocInterface {

	public final static JoinDocCollector EMPTY = new JoinDocCollector(null, 0);

	private final int maxDoc;
	private final int[] ids;
	private final int[][] foreignDocIdsArray;
	private OpenBitSet bitSet;
	private final int joinResultSize;

	public JoinDocCollector(DocIdInterface docs, int joinResultSize) {
		this.bitSet = null;
		this.maxDoc = docs.getMaxDoc();
		this.ids = ArrayUtils.clone(docs.getIds());
		this.foreignDocIdsArray = new int[ids.length][];
		this.joinResultSize = joinResultSize;
	}

	/**
	 * Copy only the valid item (other than -1)
	 * 
	 * @param src
	 */
	protected JoinDocCollector(JoinDocCollector src) {
		this.joinResultSize = src.joinResultSize;
		this.bitSet = null;
		this.maxDoc = src.maxDoc;
		int i1 = 0;
		for (int id : src.ids)
			if (id != -1)
				i1++;
		ids = new int[i1];
		foreignDocIdsArray = new int[ids.length][];
		i1 = 0;
		int i2 = 0;
		for (int id : src.ids) {
			if (id != -1) {
				ids[i1] = id;
				foreignDocIdsArray[i1++] = src.foreignDocIdsArray[i2];
			}
			i2++;
		}
	}

	@Override
	public DocIdInterface duplicate() {
		return new JoinDocCollector(this);
	}

	@Override
	public void swap(int pos1, int pos2) {
		int id = ids[pos1];
		ids[pos1] = ids[pos2];
		ids[pos2] = id;
		int[] foreignDocIds = foreignDocIdsArray[pos1];
		foreignDocIdsArray[pos1] = foreignDocIdsArray[pos2];
		foreignDocIdsArray[pos2] = foreignDocIds;
	}

	@Override
	public int[] getIds() {
		return ids;
	}

	@Override
	public int getNumFound() {
		return ids.length;
	}

	@Override
	public OpenBitSet getBitSet() {
		if (bitSet != null)
			return bitSet;
		bitSet = new OpenBitSet(maxDoc);
		for (int id : ids)
			bitSet.fastSet(id);
		return bitSet;
	}

	@Override
	public int getMaxDoc() {
		return maxDoc;
	}

	@Override
	public void setForeignDocId(int pos, int joinResultPos, int foreignDocId) {
		int[] foreignDocIds = foreignDocIdsArray[pos];
		if (foreignDocIds == null)
			foreignDocIds = new int[joinResultSize];
		foreignDocIds[joinResultPos] = foreignDocId;
		foreignDocIdsArray[pos] = foreignDocIds;
	}

	@Override
	public int getForeignDocIds(int pos, int joinPosition) {
		int[] foreignDocIds = foreignDocIdsArray[pos];
		if (foreignDocIds == null)
			return -1;
		if (joinPosition >= foreignDocIds.length)
			return -1;
		return foreignDocIds[joinPosition];
	}

	final public static DocIdInterface join(DocIdInterface docs,
			StringIndex doc1StringIndex, DocIdInterface docs2,
			StringIndex doc2StringIndex, int joinResultSize, int joinResultPos,
			Timer timer) {
		if (docs.getNumFound() == 0 || docs2.getNumFound() == 0)
			return JoinDocCollector.EMPTY;

		Timer t = new Timer(timer, "copy & sort local documents");
		JoinDocInterface docs1 = new JoinDocCollector(docs, joinResultSize);
		new AscStringIndexSorter(doc1StringIndex).quickSort(docs1, t);
		t.duration();

		t = new Timer(timer, "copy & sort foreign documents");
		docs2 = docs2.duplicate();
		new AscStringIndexSorter(doc2StringIndex).quickSort(docs2, t);
		t.duration();

		t = new Timer(timer, "join operation");
		int i1 = 0;
		int i2 = 0;
		int[] ids1 = docs1.getIds();
		int[] ids2 = docs2.getIds();
		while (i1 != ids1.length) {
			int id1 = ids1[i1];
			int id2 = ids1[i2];
			String t1 = doc1StringIndex.lookup[doc1StringIndex.order[id1]];
			String t2 = doc2StringIndex.lookup[doc2StringIndex.order[id2]];
			int c = StringUtils.compareNullString(t1, t2);
			if (c < 0) {
				ids1[i1] = -1;
				i1++;
			} else if (c > 0) {
				i2++;
				if (i2 == ids2.length) {
					while (i1 != ids1.length)
						ids1[i1++] = -1;
				}
			} else {
				docs1.setForeignDocId(i1, joinResultPos, id2);
				i1++;
			}
		}
		t.duration();

		return docs1.duplicate();
	}

}
