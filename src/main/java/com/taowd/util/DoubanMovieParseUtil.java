package com.taowd.util;

import com.alibaba.fastjson.JSON;
import com.taowd.config.DouBanHttpGetUtil;
import com.taowd.pojo.DoubanMovieBeanMySql;
import com.taowd.pojo.Movie;

public class DoubanMovieParseUtil {
	// @Autowired
	// private DoubanMovieService doubanMovieService;

	public static void main(String[] args) throws Exception {

		// // 开始解析电影详情
		// List<DoubanMovieBeanMySql> movieUrlList = doubanMovieService.selectAll();
		DoubanMovieBeanMySql item = new DoubanMovieBeanMySql();
		item.setUrl("https://movie.douban.com/subject/7916027/");
		//
		Movie result = DouBanHttpGetUtil.extractMovie(item);
		//
		System.out.println(JSON.toJSONString(result));

	}
}
