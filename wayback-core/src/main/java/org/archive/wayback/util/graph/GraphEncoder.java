/* GraphEncoder
 *
 * $Id$:
 *
 * Created on Apr 9, 2010.
 *
 * Copyright (C) 2006 Internet Archive.
 *
 * This file is part of Wayback.
 *
 * Wayback is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * any later version.
 *
 * Wayback is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with Wayback; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.archive.wayback.util.graph;

/**
 * @author brad
 *
 */
public class GraphEncoder {
	private static String DELIM = "_";
	private static String REGION_DELIM = ":";
	
	/**
	 * convert a String-encoded graph into a usable Graph object, using 
	 * default GraphConfiguration
	 * @param encodedGraph String encoded graph, as returned by getEncoded()
	 * @return a Graph, ready to use
	 * @throws GraphEncodingException if there were problems with the encoded 
	 * data
	 */
	public static Graph decode(String encodedGraph) 
	throws GraphEncodingException {
		return decode(encodedGraph, new GraphConfiguration());
	}
	/**
	 * convert a String-encoded graph into a usable Graph object, using 
	 * the provided GraphConfiguration.
	 * @param encodedGraph String encoded graph, as returned by getEncoded()
	 * @param config the GraphConfiguration to use
	 * @return a Graph, ready to use
	 * @throws GraphEncodingException if there were problems with the encoded 
	 * data
	 */
	public static Graph decode(String encodedGraph, GraphConfiguration config) 
	throws GraphEncodingException {
		// encoded = "800_35_REGIONDATA_REGIONDATA_REGIONDATA_REGIONDATA_..."
		String parts[] = encodedGraph.split(DELIM);
		int numRegions = parts.length - 2;
		if(parts.length < 1) {
			throw new GraphEncodingException("No regions defined!");
		}
		int width;
		int height;
		try {
			width = Integer.parseInt(parts[0]);
		} catch(NumberFormatException e) {
			throw new GraphEncodingException("Bad integer width:" + parts[0]);
		}
		try {
			height = Integer.parseInt(parts[1]);
		} catch(NumberFormatException e) {
			throw new GraphEncodingException("Bad integer width:" + parts[0]);
		}
		RegionData data[] = new RegionData[numRegions];
		for(int i = 0; i < numRegions; i++) {
			// REGIONDATA = "2001:-1:0ab3f70023f902f"
			//               LABEL:ACTIVE_IDX:HEXDATA
			String regionParts[] = parts[i + 2].split(REGION_DELIM);
			if(regionParts.length != 3) {
				throw new GraphEncodingException("Wrong number of parts in " + 
						parts[i+2]);
			}
			int highlightedValue = Integer.parseInt(regionParts[1]);
			int values[] = decodeHex(regionParts[2]);
			data[i] = new RegionData(regionParts[0], highlightedValue, values);
		}
		return new Graph(width, height, data, config);
	}

	/**
	 * Convert a complete Graph into an opaque String that can later be 
	 * re-assembled into a Graph object. Note that GraphConfiguration 
	 * information is NOT encoded into the opaque String.
	 * @param g Graph to encode
	 * @return opaque String which can later be used with decode()
	 */
	public static String encode(Graph g) {
		RegionGraphElement rge[] = g.getRegions();
		RegionData data[] = new RegionData[rge.length];
		for(int i = 0; i < data.length; i++) {
			data[i] = rge[i].getData();
		}
		return encode(g.width, g.height, data);
	}
	
	/**
	 * Convert a Graph fields into an opaque String that can later be 
	 * re-assembled into a Graph object. Note that GraphConfiguration 
	 * information is NOT encoded into the opaque String.
	 * @param width of the Graph
	 * @param height of the Graph
	 * @param data array of RegionData for the graph
	 * @return opaque String which can later be used with decode()
	 */
	public static String encode(int width, int height, RegionData data[]) {
		StringBuilder sb = new StringBuilder();
		sb.append(width).append(DELIM);
		sb.append(height);
		boolean first = false;
		for(RegionData datum : data) {
			if(first) {
				first = false;
			} else {
				sb.append(DELIM);
			}
			sb.append(datum.getLabel()).append(REGION_DELIM);
			sb.append(datum.getHighlightedValue()).append(REGION_DELIM);
			sb.append(encodeHex(datum.getValues()));
		}
		return sb.toString();
	}

	private static String encodeHex(int values[]) {
		StringBuilder sb = new StringBuilder(values.length);
		for(int value : values) {
			if((value > 15) || (value < 0)){
				throw new IllegalArgumentException();
			}
			sb.append(Integer.toHexString(value));
		}
		return sb.toString();
	}

	private static int[] decodeHex(String hexString) {
		int length = hexString.length();
		int values[] = new int[length];
		for(int i = 0; i < length; i++) {
			char c = hexString.charAt(i);
			if(c >= '0') {
				if(c <= '9') {
					values[i] = c - '0';
				} else {
					if(c > 'f') {
						throw new IllegalArgumentException();						
					} else {
						if(c >= 'a') {
							values[i] = c - 'W';
						} else {
							throw new IllegalArgumentException();							
						}
					}
				}
			} else {
				throw new IllegalArgumentException();
			}
		}
		return values;
	}
}