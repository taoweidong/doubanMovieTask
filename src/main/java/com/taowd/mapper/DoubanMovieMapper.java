package com.taowd.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.taowd.pojo.DoubanMovieBean;
import com.taowd.pojo.DoubanMovieBeanMySql;

@Mapper
public interface DoubanMovieMapper {
	Integer insertData(@Param("paramData") List<DoubanMovieBeanMySql> paramData);

	List<DoubanMovieBean> selectAll();

	/**
	 * 清空所有旧数据.
	 * @return
	 */
	Integer deleteAllData();
}
