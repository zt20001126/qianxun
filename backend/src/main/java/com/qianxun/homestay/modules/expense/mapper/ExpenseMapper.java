package com.qianxun.homestay.modules.expense.mapper;

import com.qianxun.homestay.modules.expense.entity.ExpenseDailyAmountEntity;
import com.qianxun.homestay.modules.expense.entity.ExpenseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 经营支出数据访问接口，SQL 统一维护在 MyBatis XML。 */
@Mapper
public interface ExpenseMapper {

    /** 按日期范围和关键词分页查询支出。 */
    List<ExpenseEntity> findPage(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("keyword") String keyword,
            @Param("offset") long offset,
            @Param("size") int size);

    /** 统计支出列表符合条件的总数。 */
    long count(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("keyword") String keyword);

    /** 按主键读取支出详情。 */
    ExpenseEntity findById(@Param("id") Long id);

    /** 新增一笔不可编辑、不可删除的经营支出。 */
    int insert(ExpenseEntity expense);

    /** 汇总指定日期区间的支出总额。 */
    BigDecimal sumBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /** 统计指定日期区间的支出笔数。 */
    long countBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /** 按实际支出日期分组汇总。 */
    List<ExpenseDailyAmountEntity> sumDailyBetween(
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
