package com.taowd.pojo;

import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * Created by Jackie on 2016/9/25 0025.
 */
@Component
@Setter
@Getter
public class Movie {
	private String movieId;
	private String name;
	private String director;
	private String scenarist;
	private String actors;
	private String type;
	private String country;
	private String language;
	private String releaseDate;
	private String runtime;
	private String ratingNum;
	private String tags;
	/**
	 * 详情链接
	 */
	private String url;
	/**
	 * 剧情简介.
	 */
	private String summary;
	/**
	 * 评价人数.
	 */
	private String votes;

}
