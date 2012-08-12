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

package com.jaeksoft.searchlib.sort;

import com.jaeksoft.searchlib.result.collector.DocIdInterface;

public class QuickSort {

	private final SorterAbstract sorter;

	public QuickSort(SorterAbstract sorter) {
		this.sorter = sorter;
	}

	private final void quicksort(DocIdInterface docIdInterface, int low,
			int high) {
		int i = low, j = high;
		// Get the pivot element from the middle of the list
		int pivot = low + (high - low) / 2;

		// Divide into two lists
		while (i <= j) {
			// If the current value from the left list is smaller then the pivot
			// element then get the next element from the left list
			while (sorter.compare(i, pivot) < 0) {
				i++;
			}
			// If the current value from the right list is larger then the pivot
			// element then get the next element from the right list
			while (sorter.compare(j, pivot) > 0) {
				j--;
			}

			// If we have found a values in the left list which is larger then
			// the pivot element and if we have found a value in the right list
			// which is smaller then the pivot element then we exchange the
			// values.
			// As we are done we can increase i and j
			if (i <= j) {
				docIdInterface.swap(i, j);
				i++;
				j--;
			}
		}
		// Recursion
		if (low < j)
			quicksort(docIdInterface, low, j);
		if (i < high)
			quicksort(docIdInterface, i, high);
	}

	public final void sort(DocIdInterface docIdInterface) {
		if (docIdInterface == null)
			return;
		int size = docIdInterface.getNumFound();
		if (size == 0)
			return;
		quicksort(docIdInterface, 0, size - 1);
	}

}
