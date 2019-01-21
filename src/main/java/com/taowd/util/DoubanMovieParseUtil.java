package com.taowd.util;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.fastjson.JSON;
import com.taowd.config.DouBanHttpGetUtil;
import com.taowd.pojo.Movie;

public class DoubanMovieParseUtil {

	public static void main(String[] args) throws Exception {

		List<String> urlList = new ArrayList<String>();
		urlList.add("https://movie.douban.com/subject/10428476/");

		String content = DouBanHttpGetUtil.getByString(urlList);

		Movie result = DouBanHttpGetUtil.extractMovie("https://movie.douban.com/", content);

		System.out.println(JSON.toJSONString(result));

	}
}
