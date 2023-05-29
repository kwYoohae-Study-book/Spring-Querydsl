package study.querydsl.repository;

import static org.springframework.util.StringUtils.*;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.*;

import java.util.List;

import javax.persistence.EntityManager;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.data.support.PageableExecutionUtils;

import com.querydsl.core.QueryResults;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;

import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.Member;

public class MemberRepositoryImpl
	// extends QuerydslRepositorySupport
	implements MemberRepositoryCustom{

	private final JPAQueryFactory queryFactory;

	public MemberRepositoryImpl(EntityManager em) {
		this.queryFactory = new JPAQueryFactory(em);
	}

	// public MemberRepositoryImpl(EntityManager em) {
	// 	super(Member.class);
	//  this.queryFactory = new JPAQueryFactory(em);
	// }

	@Override
	public List<MemberTeamDto> search(MemberSearchCondition condition) {

		// return from(member)
		// 	.leftJoin(member.team, team)
		// 	.where(
		// 		usernameEq(condition.getUsername()),
		// 		teamNameEq(condition.getTeamName()),
		// 		ageGoe(condition.getAgeGoe()),
		// 		ageLoe(condition.getAgeLoe())
		// 	).select(new QMemberTeamDto(
		// 		member.id.as("memberId"), member.username
		// 		, member.age, team.id.as("teamId"),
		// 		team.name.as("teamName")
		// 	)).fetch();

		return queryFactory
			.select(new QMemberTeamDto(
				member.id.as("memberId"), member.username
				,member.age, team.id.as("teamId"),
				team.name.as("teamName")
			))
			.from(member)
			.leftJoin(member.team, team)
			.where(
				usernameEq(condition.getUsername()),
				teamNameEq(condition.getTeamName()),
				ageGoe(condition.getAgeGoe()),
				ageLoe(condition.getAgeLoe())
			)
			.fetch();
	}

	@Override
	public Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable) {
		QueryResults<MemberTeamDto> results = queryFactory
			.select(new QMemberTeamDto(
				member.id.as("memberId"), member.username
				, member.age, team.id.as("teamId"),
				team.name.as("teamName")
			))
			.from(member)
			.leftJoin(member.team, team)
			.where(
				usernameEq(condition.getUsername()),
				teamNameEq(condition.getTeamName()),
				ageGoe(condition.getAgeGoe()),
				ageLoe(condition.getAgeLoe())
			)
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.fetchResults();

		List<MemberTeamDto> content = results.getResults();
		long total = results.getTotal();

		return new PageImpl<>(content, pageable, total);
	}

	// public Page<MemberTeamDto> searchPageSimple2(MemberSearchCondition condition, Pageable pageable) {
	//
	// 	JPQLQuery<MemberTeamDto> jpaQuery = from(member)
	// 		.leftJoin(member.team, team)
	// 		.where(
	// 			usernameEq(condition.getUsername()),
	// 			teamNameEq(condition.getTeamName()),
	// 			ageGoe(condition.getAgeGoe()),
	// 			ageLoe(condition.getAgeLoe())
	// 		).select(new QMemberTeamDto(
	// 			member.id.as("memberId"), member.username
	// 			, member.age, team.id.as("teamId"),
	// 			team.name.as("teamName")
	// 		));
	//
	// 	JPQLQuery<MemberTeamDto> query = getQuerydsl().applyPagination(pageable, jpaQuery);
	//
	// 	QueryResults<MemberTeamDto> member = query.fetchResults();
	//
	// 	List<MemberTeamDto> content = results.getResults();
	// 	long total = results.getTotal();
	//
	// 	return new PageImpl<>(content, pageable, total);
	// }

	@Override
	public Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable) {
		List<MemberTeamDto> content = queryFactory
			.select(new QMemberTeamDto(
				member.id.as("memberId"), member.username
				, member.age, team.id.as("teamId"),
				team.name.as("teamName")
			))
			.from(member)
			.leftJoin(member.team, team)
			.where(
				usernameEq(condition.getUsername()),
				teamNameEq(condition.getTeamName()),
				ageGoe(condition.getAgeGoe()),
				ageLoe(condition.getAgeLoe())
			)
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.fetch();

		// join이 필요없이 simple하게 count를 할 수 있는 경우가 존재 , 이렇게 하면 좀더 최적화가능
		JPAQuery<MemberTeamDto> countQuery = queryFactory
			.select(new QMemberTeamDto(
				member.id.as("memberId"), member.username
				, member.age, team.id.as("teamId"),
				team.name.as("teamName")
			))
			.from(member)
			.leftJoin(member.team, team)
			.where(
				usernameEq(condition.getUsername()),
				teamNameEq(condition.getTeamName()),
				ageGoe(condition.getAgeGoe()),
				ageLoe(condition.getAgeLoe())
			);

		// 이렇게하면, getPage에서 totalSize를 보고, 시작이거나 크면 함수 자체를 실행하지 않음
		return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchCount);
	}

	private BooleanExpression usernameEq(String username) {
		return hasText(username) ? member.username.eq(username) : null;
	}

	private BooleanExpression teamNameEq(String teamName) {
		return hasText(teamName) ? team.name.eq(teamName) : null;
	}

	private BooleanExpression ageGoe(Integer ageGoe) {
		return ageGoe != null ? member.age.goe(ageGoe) : null;
	}

	private BooleanExpression ageLoe(Integer ageLoe) {
		return ageLoe != null ? member.age.loe(ageLoe) : null;
	}
}
