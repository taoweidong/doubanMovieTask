package com.taowd.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.taowd.mapper.DoubanMovieMapper;
import com.taowd.pojo.DoubanMovieBean;
import com.taowd.pojo.DoubanMovieBeanMySql;

@Service
public class DoubanMovieService {

	@Autowired
	private DoubanMovieMapper doubanMovieMapper;

	public Integer insertData(List<DoubanMovieBean> paramData) {
		return doubanMovieMapper.insertData(dealData(paramData));
	}

	public Integer deleteAllData() {
		return doubanMovieMapper.deleteAllData();
	}

	private List<DoubanMovieBeanMySql> dealData(List<DoubanMovieBean> paramData) {
		List<DoubanMovieBeanMySql> param = new ArrayList<DoubanMovieBeanMySql>();

		paramData.forEach(x -> {
			DoubanMovieBeanMySql item = new DoubanMovieBeanMySql();
			item.setId(x.getId());
			item.setTitle(x.getTitle());
			item.setUrl(x.getUrl());
			item.setDirectors(String.join(",", x.getDirectors()));
			item.setRate(x.getRate());
			item.setStar(x.getStar());
			item.setCasts(String.join(",", x.getCasts()));
			item.setCover(x.getCover());
			item.setCover_x(x.getCover_x());
			item.setCover_y(x.getCover_y());

			param.add(item);
		});

		return param;
	}
}
