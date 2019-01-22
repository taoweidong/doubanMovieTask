package com.taowd.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.taowd.pojo.DoubanMovieBeanMySql;
import com.taowd.pojo.Movie;

@Mapper
public interface DoubanMovieMapper {

	Integer insertMovieDetail(Movie paramData);

	Integer insertData(@Param("paramData") List<DoubanMovieBeanMySql> paramData);

	List<DoubanMovieBeanMySql> selectAll();

	/**
	 * 清空所有旧数据.
	 * @return
	 */
	Integer deleteAllData();

	/**
	 * 清空所有旧数据.
	 * @return
	 */
	Integer deleteMovieDetail();
}