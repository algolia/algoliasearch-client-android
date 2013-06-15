package com.algolia.search;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

/*
 * Copyright (c) 2013 Algolia
 * http://www.algolia.com/
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

public class QueryParameters {
	protected List<String> attributes;
	protected List<String> attributesToHighlight;
	protected int minWordSizeForApprox1;
	protected int minWordSizeForApprox2;
	protected boolean getRankingInfo;
	protected int page;
	protected int hitsPerPage;
	protected String tags;
	protected String insideBoundingBox;
	protected String aroundLatLong;
	protected String query;
	
	public QueryParameters(String query) {
		minWordSizeForApprox1 = 3;
		minWordSizeForApprox2 = 7;
		getRankingInfo = false;
		page = 0;
		hitsPerPage = 20;
		this.query = query;
	}
	
	public QueryParameters() {
		minWordSizeForApprox1 = 3;
		minWordSizeForApprox2 = 7;
		getRankingInfo = false;
		page = 0;
		hitsPerPage = 20;
	}
	
	/**
	 * Set the full text query
	 */
	public QueryParameters setQueryString(String query)
	{
		this.query = query;
		return this;
	}
	
	/**
	 * Specify the list of attribute names to retrieve. 
     * By default all attributes are retrieved.
	 */
	public QueryParameters setAttributesToRetrieve(List<String> attributes) {
		this.attributes = attributes;
		return this;
	}

	/**
	 * Specify the list of attribute names to highlight. 
     * By default indexed attributes are highlighted.
	 */
	public QueryParameters setAttributesToHighlight(List<String> attributes) {
		this.attributesToHighlight = attributes;
		return this;
	}

	/**
	 * Specify the minimum number of characters in a query word to accept one typo in this word. 
     * Defaults to 3.
	 */
	public QueryParameters setMinWordSizeToAllowOneTypo(int nbChars) {
		minWordSizeForApprox1 = nbChars;
		return this;
	}
	
	/**
	 * Specify the minimum number of characters in a query word to accept two typos in this word. 
     * Defaults to 7.
	 */
	public QueryParameters setMinWordSizeToAllowTwoTypos(int nbChars) {
		minWordSizeForApprox2 = nbChars;
		return this;
	}

	/**
	 * if set, the result hits will contain ranking information in _rankingInfo attribute.
	 */
	public QueryParameters getRankingInfo(boolean enabled) {
		getRankingInfo = enabled;
		return this;
	}

	/**
	 * Set the page to retrieve (zero base). Defaults to 0.
	 */
	public QueryParameters setPage(int page) {
		this.page = page;
		return this;
	}
	
	/**
	 *  Set the number of hits per page. Defaults to 10.
	 */
	public QueryParameters setNbHitsPerPage(int nbHitsPerPage) {
		this.hitsPerPage = nbHitsPerPage;
		return this;
	}
	
	/**
	 *  Search for entries around a given latitude/longitude. 
     *  @param radius set the maximum distance in meters.
     *  Note: at indexing, geoloc of an object should be set with _geoloc attribute containing lat and lng attributes (for example {"_geoloc":{"lat":48.853409, "lng":2.348800}})
	 */
	public QueryParameters aroundLatitudeLongitude(float latitude, float longitude, int radius) {
		aroundLatLong = "aroundLatLng=" + latitude + "," + longitude + "&aroundRadius=" + radius;
		return this;
	}
	
	/**
	 *  Search for entries inside a given area defined by the two extreme points of a rectangle.
     *    At indexing, geoloc of an object should be set with _geoloc attribute containing lat and lng attributes (for example {"_geoloc":{"lat":48.853409, "lng":2.348800}})
	 */
	public QueryParameters insideBoundingBox(float latitudeP1, float longitudeP1, float latitudeP2, float longitudeP2) {
		insideBoundingBox = "insideBoundingBox=" + latitudeP1 + "," + longitudeP1 + "," + latitudeP2 + "," + longitudeP2;
		return this;
	}
	
	/**
	 * Filter the query by a set of tags. You can AND tags by separating them by commas. To OR tags, you must add parentheses. For example tag1,(tag2,tag3) means tag1 AND (tag2 OR tag3).
	 * At indexing, tags should be added in the _tags attribute of objects (for example {"_tags":["tag1","tag2"]} )
	 */
	public QueryParameters setTags(String tags) {
		this.tags = tags;
		return this;
	}
	
	protected String getQueryString() {
		StringBuilder stringBuilder = new StringBuilder();
		
		try {
			if (attributes != null) {
				stringBuilder.append("attributes=");
				boolean first = true;
				for (String attr : this.attributes) {
					if (!first)
						stringBuilder.append(",");
					stringBuilder.append(URLEncoder.encode(attr, "UTF-8"));
					first = false;
				}
			}
			if (attributesToHighlight != null) {
				if (stringBuilder.length() > 0)
					stringBuilder.append('&');
				stringBuilder.append("attributesToHighlight=");
				boolean first = true;
				for (String attr : this.attributesToHighlight) {
					if (!first)
						stringBuilder.append(',');
					stringBuilder.append(URLEncoder.encode(attr, "UTF-8"));
					first = false;
				}
			}
			if (minWordSizeForApprox1 != 3) {
				if (stringBuilder.length() > 0)
					stringBuilder.append('&');
				stringBuilder.append("minWordSizeForApprox1=");
				stringBuilder.append(minWordSizeForApprox1);
			}
			if (minWordSizeForApprox2 != 7) {
				if (stringBuilder.length() > 0)
					stringBuilder.append('&');
				stringBuilder.append("minWordSizeForApprox2=");
				stringBuilder.append(minWordSizeForApprox2);
			}
			if (getRankingInfo) {
				if (stringBuilder.length() > 0)
					stringBuilder.append('&');
				stringBuilder.append("getRankingInfo=1");
			}
			if (page > 0) {
				if (stringBuilder.length() > 0)
					stringBuilder.append('&');
				stringBuilder.append("page=");
				stringBuilder.append(page);
			}
			if (hitsPerPage != 20 && hitsPerPage > 0) {
				if (stringBuilder.length() > 0)
					stringBuilder.append('&');
				stringBuilder.append("hitsPerPage=");
				stringBuilder.append(hitsPerPage);
			}
			if (tags != null) {
				if (stringBuilder.length() > 0)
					stringBuilder.append('&');
				stringBuilder.append("tags=");
				stringBuilder.append(URLEncoder.encode(tags, "UTF-8"));
			}
			if (insideBoundingBox != null) {
				if (stringBuilder.length() > 0)
					stringBuilder.append('&');
				stringBuilder.append(insideBoundingBox);
			} else if (aroundLatLong != null) {
				if (stringBuilder.length() > 0)
					stringBuilder.append('&');
				stringBuilder.append(aroundLatLong);
			}
			if (query != null) {
				if (stringBuilder.length() > 0)
					stringBuilder.append('&');
				stringBuilder.append("query=");
				stringBuilder.append(URLEncoder.encode(query, "UTF-8"));
			}
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		return stringBuilder.toString();
	}
}
