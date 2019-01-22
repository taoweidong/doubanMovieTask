package com.taowd.config;

import java.io.IOException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Jackie on 2016/9/24 0024.
 */

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.htmlparser.Node;
import org.htmlparser.Parser;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeList;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.alibaba.fastjson.JSON;
import com.taowd.pojo.DoubanMovieBeanMySql;
import com.taowd.pojo.Movie;
import com.taowd.util.Constants;

public class DouBanHttpGetUtil {

	protected static Movie movie = new Movie();
	public static int movieId = 0;
	public static int commentId = 0;

	private final static String getByString(String url) throws Exception {

		String responseBody = "";
		CloseableHttpClient httpClient = HttpClients.createDefault();
		try {

			HttpGet httpGet = new HttpGet(url);
			System.out.println("executing request " + httpGet.getURI());

			ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

				public String handleResponse(final HttpResponse response)
						throws ClientProtocolException, IOException {
					int status = response.getStatusLine().getStatusCode();
					System.out.println("------------status:" + status);
					if (status >= 200 && status < 300) {
						HttpEntity entity = response.getEntity();
						return entity != null ? EntityUtils.toString(entity) : null;
					} else if (status == 300 || status == 301 || status == 302 || status == 304
							|| status == 400 || status == 401 || status == 403 || status == 404
							|| new String(status + "").startsWith("5")) { // refer to link
																			// http://blog.csdn.net/u012043391/article/details/51069441
						return null;
					} else {
						throw new ClientProtocolException("Unexpected response status: " + status);
					}
				}
			};

			friendlyToDouban();
			responseBody = httpClient.execute(httpGet, responseHandler);

		} finally {
			httpClient.close();
		}

		return responseBody;
	}

	public static Movie extractMovie(DoubanMovieBeanMySql param) throws Exception {
		String content = DouBanHttpGetUtil.getByString(param.getUrl());

		System.out.println("==========Parse Movie:" + param.getUrl() + "============");

		Document movieDoc = Jsoup.parse(content);
		if (movieDoc.html().contains("导演") && movieDoc.html().contains("主演")
				&& movieDoc.html().contains("类型") && movieDoc.html().contains("语言")) {
			Elements infos = movieDoc.getElementById("info").children();
			movie.setMovieId(param.getId());
			movie.setUrl(param.getUrl());
			for (Element info : infos) {
				if (info.childNodeSize() > 0) {
					String key = info.getElementsByAttributeValue("class", "pl").text();
					if ("导演".equals(key)) {
						movie.setDirector(
								info.getElementsByAttributeValue("class", "attrs").text());
					} else if ("编剧".equals(key)) {
						movie.setScenarist(
								info.getElementsByAttributeValue("class", "attrs").text());
					} else if ("主演".equals(key)) {
						movie.setActors(info.getElementsByAttributeValue("class", "attrs").text());
					} else if ("类型:".equals(key)) {
						movie.setType(
								movieDoc.getElementsByAttributeValue("property", "v:genre").text());
					} else if ("制片国家/地区:".equals(key)) {
						Pattern patternCountry = Pattern.compile(
								".制片国家/地区:</span>.+[\\u4e00-\\u9fa5]+.+[\\u4e00-\\u9fa5]+\\s+<br>");
						Matcher matcherCountry = patternCountry.matcher(movieDoc.html());
						if (matcherCountry.find()) {
							movie.setCountry(
									matcherCountry.group().split("</span>")[1].split("<br>")[0]
											.trim());// for example: >制片国家/地区:</span> 中国大陆 / 香港
														// <br>
						}
					} else if ("语言:".equals(key)) {
						Pattern patternLanguage = Pattern.compile(
								".语言:</span>.+[\\u4e00-\\u9fa5]+.+[\\u4e00-\\u9fa5]+\\s+<br>");
						Matcher matcherLanguage = patternLanguage.matcher(movieDoc.html());
						if (matcherLanguage.find()) {
							movie.setLanguage(
									matcherLanguage.group().split("</span>")[1].split("<br>")[0]
											.trim());
						}
					} else if ("上映日期:".equals(key)) {
						movie.setReleaseDate(movieDoc
								.getElementsByAttributeValue("property", "v:initialReleaseDate")
								.text());
					} else if ("片长:".equals(key)) {
						movie.setRuntime(movieDoc
								.getElementsByAttributeValue("property", "v:runtime").text());
					}
				}
			}
			movie.setTags(movieDoc.getElementsByClass("tags-body").text());
			movie.setName(
					movieDoc.getElementsByAttributeValue("property", "v:itemreviewed").text());
			movie.setRatingNum(
					movieDoc.getElementsByAttributeValue("property", "v:average").text());
		}

		movie.setSummary(movieDoc.getElementsByAttributeValue("property", "v:summary").text());
		movie.setVotes(movieDoc.getElementsByAttributeValue("property", "v:votes").text());
		return movie;
	}

	public static void parseFromString(String content) throws Exception {

		Parser parser = new Parser(content);
		HasAttributeFilter filter = new HasAttributeFilter("href");

		List<String> nextLinkList = new ArrayList<String>();

		int rowCount = 0;

		if (rowCount <= Constants.maxCycle) { // once rowCount is bigger than maxCycle, the new
												// crawled link will not insert into record table
			try {
				NodeList list = parser.parse(filter);
				int count = list.size();

				// process every link on this page
				for (int i = 0; i < count; i++) {
					Node node = list.elementAt(i);

					if (node instanceof LinkTag) {
						LinkTag link = (LinkTag) node;
						String nextLink = link.extractLink();
						String mainUrl = Constants.MAINURL;

						if (nextLink.startsWith(mainUrl)) {
							Pattern moviePattern = Pattern.compile(Constants.MOVIE_REGULAR_EXP);
							Matcher movieMatcher = moviePattern.matcher(nextLink);

							Pattern commentPattern = Pattern.compile(Constants.COMMENT_REGULAR_EXP);
							Matcher commentMatcher = commentPattern.matcher(nextLink);

							if (movieMatcher.find() || commentMatcher.find()) {
								nextLinkList.add(nextLink);
							}
						}
					}
				}

			} catch (Exception e) {
				// handle the exceptions
				e.printStackTrace();
				System.out.println("SQLException: " + e.getMessage());
			}
		}

		System.out.println(JSON.toJSONString(nextLinkList));
	}

	public final static void getByString(List<String> urlList, Connection conn) throws Exception {
		CloseableHttpClient httpClient = HttpClients.createDefault();
		try {
			for (String url : urlList) {
				HttpGet httpGet = new HttpGet(url);
				System.out.println("executing request " + httpGet.getURI());

				ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

					public String handleResponse(final HttpResponse response)
							throws ClientProtocolException, IOException {
						int status = response.getStatusLine().getStatusCode();
						System.out.println("------------status:" + status);
						if (status >= 200 && status < 300) {
							HttpEntity entity = response.getEntity();
							return entity != null ? EntityUtils.toString(entity) : null;
						} else if (status == 300 || status == 301 || status == 302 || status == 304
								|| status == 400 || status == 401 || status == 403 || status == 404
								|| new String(status + "").startsWith("5")) { // refer to link
																				// http://blog.csdn.net/u012043391/article/details/51069441
							return null;
						} else {
							throw new ClientProtocolException(
									"Unexpected response status: " + status);
						}
					}
				};

				friendlyToDouban();
				String responseBody = httpClient.execute(httpGet, responseHandler);

				if (responseBody != null) {
					// DouBanParsePage.parseFromString(responseBody, conn);// analyze all links and
					// save into DB
					// DouBanParsePage.extractMovie(url, responseBody, conn);// analyze the page and
					// // save into DB if the
					// // current page is movie
					// // detail page
					// DouBanParsePage.extractComment(url, responseBody, conn);// analyze the page
					// and
					// // save into DB if the
					// current page is
					// comment detail page
				}
			}
		} finally {
			httpClient.close();
		}
	}

	private static void friendlyToDouban() throws InterruptedException {
		Thread.sleep((new Random().nextInt(10) + 1) * 1000);// sleep for the random second so that
															// avoiding to be listed into blacklist
	}
}
