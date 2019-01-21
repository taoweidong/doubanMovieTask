package com.taowd.pojo;

import java.util.List;

import lombok.Data;

@Data
public class DoubanMovieBean {

	private String id;
	private String title;
	private String url;
	private List<String> directors;
	private String rate;
	private String star;
	private List<String> casts;
	private String cover;
	private String cover_x;
	private String cover_y;

}
