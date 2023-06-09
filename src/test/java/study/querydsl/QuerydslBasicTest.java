package study.querydsl;

import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.*;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.experimental.StandardException;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;
import study.querydsl.entity.Team;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

	@Autowired
	EntityManager em;

	JPAQueryFactory queryFactory;

	@BeforeEach
	void before() {
		queryFactory = new JPAQueryFactory(em);
		Team teamA = new Team("teamA");
		Team teamB = new Team("teamB");
		em.persist(teamA);
		em.persist(teamB);

		Member member1 = new Member("member1", 10, teamA);
		Member member2 = new Member("member2", 20, teamA);

		Member member3 = new Member("member3", 30, teamB);
		Member member4 = new Member("member4", 40, teamB);
		em.persist(member1);
		em.persist(member2);
		em.persist(member3);
		em.persist(member4);
	}

	@Test
	void startJPQL() {
		// member1을 찾아라
		Member findMember = em.createQuery("select m from Member m "
				+ "where m.username = :username", Member.class)
			.setParameter("username", "member1")
			.getSingleResult();

		assertThat(findMember.getUsername()).isEqualTo("member1");
	}

	@Test
	void startQuerydsl() {
		// 1단계
		// QMember m = QMember.member;
		// Member findMember = queryFactory.select(m)
		// 	.from(m)
		// 	.where(m.username.eq("member1"))
		// 	.fetchOne();

		// 2단계 static import 사용
		Member findMember = queryFactory
			.select(member)
			.from(member)
			.where(member.username.eq("member1"))
			.fetchOne();

		assertThat(findMember.getUsername()).isEqualTo("member1");
	}

	@Test
	void search() {
		Member findMember = queryFactory
			.selectFrom(member)
			.where(member.username.eq("member1").and(member.age.eq(10)))
			.fetchOne();

		assertThat(findMember.getUsername()).isEqualTo("member1");
	}

	@Test
	void searchAndParam() {
		Member findMember = queryFactory
			.selectFrom(member)
			.where(
				member.username.eq("member1"), (member.age.eq(10))
			)
			.fetchOne();

		assertThat(findMember.getUsername()).isEqualTo("member1");
	}

	@Test
	void resultFetch() {
		List<Member> fetch =
			queryFactory.selectFrom(member).fetch();

		Member fetchOne = queryFactory.selectFrom(member).fetchOne();

		Member fetchFirst = queryFactory.selectFrom(member).fetchFirst();

		QueryResults<Member> results = queryFactory.selectFrom(member).fetchResults();

		results.getTotal(); // 사용하면 Count 쿼리까지 가져와서 2번 날라감
		List<Member> content = results.getResults();

		long total = queryFactory.selectFrom(member)
			.fetchCount();
	}

	/**
	 * 회원 정렬순서
	 * 1. 나이 내림차순
	 * 2. 이름 올림차순
	 * 2에서 회원이름이 없으면 마지막에 출력 (nulls last)
	 */
	@Test
	void sort() {
		em.persist(new Member(null, 100));
		em.persist(new Member("member5", 100));
		em.persist(new Member("member6", 100));

		List<Member> result = queryFactory.selectFrom(member)
			.where(member.age.eq(100))
			.orderBy(member.age.desc(), member.username.asc().nullsLast())
			.fetch();

		Member member5 = result.get(0);
		Member member6 = result.get(1);
		Member memberNull = result.get(2);

		assertThat(member5.getUsername()).isEqualTo("member5");
		assertThat(member6.getUsername()).isEqualTo("member6");
		assertThat(memberNull.getUsername()).isNull();
	}

	@Test
	void paging1() {
		List<Member> result = queryFactory
			.selectFrom(member)
			.orderBy(member.username.desc())
			.offset(1)
			.limit(2)
			.fetch();

		assertThat(result.size()).isEqualTo(2);
	}

	@Test
	void paging2() {
		QueryResults<Member> queryResults = queryFactory
			.selectFrom(member)
			.orderBy(member.username.desc())
			.offset(1)
			.limit(2)
			.fetchResults();

		assertThat(queryResults.getTotal()).isEqualTo(4);
		assertThat(queryResults.getLimit()).isEqualTo(2);
		assertThat(queryResults.getOffset()).isEqualTo(1);
		assertThat(queryResults.getResults().size()).isEqualTo(2);
	}

	@Test
	void aggregation() {
		List<Tuple> result = queryFactory.select(member.count(),
				member.age.sum(),
				member.age.avg(),
				member.age.max(),
				member.age.min())
			.from(member)
			.fetch();

		// Tuple은 쿼리 dsl 튜플임r
		Tuple tuple = result.get(0);

		assertThat(tuple.get(member.count())).isEqualTo(4);
		assertThat(tuple.get(member.age.sum())).isEqualTo(100);
		assertThat(tuple.get(member.age.avg())).isEqualTo(25);
		assertThat(tuple.get(member.age.max())).isEqualTo(40);
		assertThat(tuple.get(member.age.min())).isEqualTo(10);
	}

	/**
	 * 팀의 이름과 각 팀의 평균 연령?
	 */
	@Test
	void groupBy() {
		List<Tuple> result = queryFactory
			.select(team.name, member.age.avg())
			.from(member)
			.join(member.team, team)
			.groupBy(team.name)
			.fetch();

		Tuple teamA = result.get(0);
		Tuple teamB = result.get(1);

		assertThat(teamA.get(team.name)).isEqualTo("teamA");
		assertThat(teamA.get(member.age.avg())).isEqualTo(15);

		assertThat(teamB.get(team.name)).isEqualTo("teamB");
		assertThat(teamB.get(member.age.avg())).isEqualTo(35);
	}

	/**
	 * 팀 A에 소속된 모든 회원
	 */
	@Test
	void join() {
		List<Member> result = queryFactory
			.selectFrom(member)
			.join(member.team, team)
			.where(team.name.eq("teamA"))
			.fetch();

		assertThat(result)
			.extracting("username")
			.containsExactly("member1", "member2");
	}

	/**
	 * 세타조인
	 * 회원의 이름이 팀 이름과 같은 회원 조인
	 * 제약 사항으로, left outer나 right outer인 외부 조인을 사용불가능 But on을 사용하면 가능
	 */
	@Test
	void theta_join() {
		em.persist(new Member("teamA"));
		em.persist(new Member("teamB"));
		em.persist(new Member("teamC"));

		List<Member> result = queryFactory
			.select(member)
			.from(member, team)
			.where(member.username.eq(team.name))
			.fetch();

		assertThat(result)
			.extracting("username")
			.containsExactly("teamA", "teamB");
	}

	/**
	 * 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인을 해라, 회원은 모두 조회해라
	 * JPQL: Select m, t from Member m left join m.team t on t.name  = 'teamA'
	 * inner join이면 on 쓰지말기
	 */
	@Test
	void join_on_filtering() {
		List<Tuple> result = queryFactory
			.select(member, team)
			.from(member)
			.leftJoin(member.team, team).on(team.name.eq("teamA"))
			.fetch();

		for (Tuple tuple : result) {
			System.out.println("tuple = " + tuple);
		}
	}

	/**
	 * 연관관계 없는 엔티티와 외부 조인할때 사용
	 * 회원의 이름과 같은 이름과 같은 대상 외부 조인
	 */
	@Test
	void join_on_no_relation() {
		em.persist(new Member("teamA"));
		em.persist(new Member("teamB"));
		em.persist(new Member("teamC"));

		List<Tuple> fetch = queryFactory
			.select(member, team)
			.from(member)
			.leftJoin(team).on(member.username.eq(team.name))
			.fetch();

		for (Tuple tuple : fetch) {
			System.out.println("tuple = " + tuple);
		}
	}

	@PersistenceUnit
	EntityManagerFactory emf;

	@Test
	void fetchJoinNo() {
		em.flush();
		em.clear();

		Member findMember = queryFactory.selectFrom(member)
			.where(member.username.eq("member1"))
			.fetchOne();

		//Lazy이기 때문에 team은 조인 안됨

		boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
		assertThat(loaded).as("페치 조인 미적용").isFalse();
	}

	@Test
	void fetchJoinUSE() {
		em.flush();
		em.clear();

		Member findMember = queryFactory.selectFrom(member)
			.join(member.team, team).fetchJoin()
			.where(member.username.eq("member1"))
			.fetchOne();

		//Lazy이기 때문에 team은 조인 안됨

		boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
		assertThat(loaded).as("페치 적용").isTrue();
	}

	/**
	 * 나이가 가장 많은 회원 조회
	 */
	@Test
	void subQuery() {

		QMember memberSub = new QMember("memberSub"); // 충돌나기 때문에 서브 쿼리르 입력

		List<Member> result = queryFactory
			.selectFrom(member)
			.where(member.age.eq(
				JPAExpressions
					.select(memberSub.age.max())
					.from(memberSub)
			))
			.fetch();

		assertThat(result).extracting("age")
			.containsExactly(40);
	}

	/**
	 * 나이가 평균 이상 회원 조회
	 */
	@Test
	void subQueryGoe() {

		QMember memberSub = new QMember("memberSub"); // 충돌나기 때문에 서브 쿼리르 입력

		List<Member> result = queryFactory
			.selectFrom(member)
			.where(member.age.goe(
				JPAExpressions
					.select(memberSub.age.avg())
					.from(memberSub)
			))
			.fetch();

		assertThat(result).extracting("age")
			.containsExactly(30, 40);
	}

	/**
	 * 나이가 10살 초과인 회원 조회
	 */
	@Test
	void subQueryIn() {

		QMember memberSub = new QMember("memberSub"); // 충돌나기 때문에 서브 쿼리르 입력

		List<Member> result = queryFactory
			.selectFrom(member)
			.where(member.age.in(
				JPAExpressions
					.select(memberSub.age)
					.from(memberSub)
					.where(memberSub.age.gt(10))
			))
			.fetch();

		assertThat(result).extracting("age")
			.containsExactly(20, 30, 40);
	}

	@Test
	void selectSubQuery() {

		QMember memberSub = new QMember("memberSub"); // 충돌나기 때문에 서브 쿼리르 입력

		List<Tuple> fetch = queryFactory
			.select(member.username,
				JPAExpressions
					.select(memberSub.age.avg())
					.from(memberSub))
			.from(member)
			.fetch();

		for (Tuple tuple : fetch) {
			System.out.println("tuple = " + tuple);
		}
	}

	@Test
	void basicCase() {
		List<String> result = queryFactory
			.select(member.age.when(10).then("열살")
				.when(20).then("스무살")
				.otherwise("기타")
			).from(member).fetch();

		for (String s : result) {
			System.out.println("s = " + s);
		}
	}

	@Test
	void complexCase() {
		List<String> result = queryFactory
			.select(new CaseBuilder()
				.when(member.age.between(0, 20)).then("0~20살")
				.when(member.age.between(21, 30)).then("21-30살")
				.otherwise("기타"))
			.from(member)
			.fetch();

		for (String s : result) {
			System.out.println("s = " + s);
		}
	}

	@Test
	void constant() {
		List<Tuple> result = queryFactory
			.select(member.username, Expressions.constant("A"))
			.from(member)
			.fetch();

		for (Tuple tuple : result) {
			System.out.println("tuple = " + tuple);
		}
	}

	// Enum은 StringValue로
	@Test
	void concat() {

		//{username}_{age}
		List<String> fetch = queryFactory
			.select(member.username.concat("_").concat(member.age.stringValue()))
			.from(member)
			.where(member.username.eq("member1"))
			.fetch();

		for (String s : fetch) {
			System.out.println("s = " + s);
		}
	}

	@Test
	void simpleProjection() {
		List<String> result = queryFactory
			.select(member.username)
			.from(member)
			.fetch();

		for (String s : result) {
			System.out.println("s = " + s);
		}
	}

	@Test
	void tupleProjection() {
		List<Tuple> result = queryFactory
			.select(member.username, member.age)
			.from(member)
			.fetch();

		for (Tuple tuple : result) {
			String username = tuple.get(member.username);
			Integer age = tuple.get(member.age);
			System.out.println("username = " + username);
			System.out.println("age = " + age);
		}
	}

	@Test
	void findDtoByJPQL() {
		List<MemberDto> result = em.createQuery(
				"select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
			.getResultList();

		for (MemberDto memberDto : result) {
			System.out.println("memberDto = " + memberDto);
		}
	}

	// 기본 생성자가 존재해야함 DTO에
	@Test
	void findDtoBySetter() {
		List<MemberDto> result = queryFactory
			.select(Projections.bean(MemberDto.class,
				member.username,
				member.age))
			.from(member)
			.fetch();

		for (MemberDto memberDto : result) {
			System.out.println("memberDto = " + memberDto);
		}
	}

	@Test
	void findDtoByFiled() {
		List<MemberDto> result = queryFactory
			.select(Projections.fields(MemberDto.class,
				member.username,
				member.age))
			.from(member)
			.fetch();

		for (MemberDto memberDto : result) {
			System.out.println("memberDto = " + memberDto);
		}
	}

	// 타입이 제대로 맞아야함
	@Test
	void findDtoByConstructor() {
		List<UserDto> result = queryFactory
			.select(Projections.constructor(UserDto.class,
				member.username,
				member.age))
			.from(member)
			.fetch();

		for (UserDto memberDto : result) {
			System.out.println("memberDto = " + memberDto);
		}
	}

	// 필드 일때는, 이름명이 안맞으면 값이 안들어감
	// name -> (X) username
	// 실행은 되는데 null 값이 나옴
	@Test
	void findUserDtoByFiled() {
		QMember memberSub = new QMember("memberSub");
		List<UserDto> result = queryFactory
			.select(Projections.fields(UserDto.class,
				member.username.as("name"),
				// member.age))
				ExpressionUtils.as(JPAExpressions
					.select(memberSub.age.max())
					.from(memberSub), "age") // sub query가 들어갈 경우는, 다음과 같이 alias
			)) // ExpressionUtils 보단 .as가 alias 정하기에 덜 복잡 -> 서브쿼리는 어쩔 수 없
			.from(member)
			.fetch();

		for (UserDto memberDto : result) {
			System.out.println("userDto = " + memberDto);
		}
	}

	// 기존 constructor는 컴파일 시점에, 오류를 잡지 못함
	// 단점: Q 파일이 생성됨, 아키텍쳐적으로, DTO가 QueryDSL에 의존하게됨
	@Test
	void findDtoByQueryProjection() {
		List<MemberDto> result = queryFactory
			.select(new QMemberDto(member.username, member.age))
			.from(member)
			.fetch();

		for (MemberDto memberDto : result) {
			System.out.println("memberDto = " + memberDto);
		}
	}

	@Test
	void dynamicQuery_BooleanBuilder() throws Exception {
		String usernameParam = "member1";
		Integer ageParam = null;

		List<Member> result = searchMember1(usernameParam, ageParam);
		assertThat(result.size()).isEqualTo(1);
	}

	private List<Member> searchMember1(String usernameCond, Integer ageCond) {

		BooleanBuilder builder = new BooleanBuilder();
		// BooleanBuilder builder = new BooleanBuilder(member.username.eq(usernameCond)); -> 초기화 가능 즉, 필수일경우
		if (usernameCond != null) {
			builder.and(member.username.eq(usernameCond));
		}

		if (ageCond != null) {
			builder.and(member.age.eq(ageCond));
		}

		return queryFactory
			.selectFrom(member)
			.where(builder)
			.fetch();
	}

	@Test
	void dynamicQuery_WhereParam() {
		String usernameParam = "member1";
		Integer ageParam = null;

		List<Member> result = searchMember2(usernameParam, ageParam);
		assertThat(result.size()).isEqualTo(1);
	}

	private List<Member> searchMember2(String usernameCond, Integer ageCond) {
		return queryFactory
			.selectFrom(member)
			.where(usernameEq(usernameCond), ageEq(ageCond)) // where에 null이 있으면, 무시가됨
			.fetch();
	}

	// 이런경우 삼항 연산자도 좋음
	private BooleanExpression usernameEq(String usernameCond) {
		return usernameCond != null ? member.username.eq(usernameCond) : null;
	}

	private BooleanExpression ageEq(Integer ageCond) {
		if (ageCond == null)
			return null;

		return member.age.eq(ageCond);
	}

	// 한방 조립도 가능, 조립할라면 BooleanExpression 사용할것
	private Predicate allEq(String usernameCond, Integer ageCond) {
		return usernameEq(usernameCond).and(ageEq(ageCond));
	}

	@Test
	// @Commit
	void bulkUpdate() {

		// member1 = 10 -> 비회원 -> 영속성 컨텍스트에는 그대로 member1
		// member2 = 20 -> 비회원 -> 영속성 컨텍스트에는 그대로 member2
		// -> 결론, DB의 상태와 영속성 컨텍스트의 값과 서로 다름
		long count = queryFactory
			.update(member)
			.set(member.username, "비회원")
			.where(member.age.lt(28))
			.execute();

		em.flush();
		em.clear();

		// 맴버가져올경우
		List<Member> result = queryFactory
			.selectFrom(member)
			.fetch();

		// 영속성 컨텐스트의 값을 가지고 있음 -> Repeatable read
		for (Member member1 : result) {
			System.out.println("member1 = " + member1);
		}
	}

	@Test
	void bulkAdd() {
		long count = queryFactory
			.update(member)
			// .set(member.age, member.age.add(1))
			.set(member.age, member.age.multiply(2))
			.execute();
	}

	@Test
	void bulkDelete() {
		long count = queryFactory
			.delete(member)
			.where(member.age.gt(18))
			.execute();
	}

	@Test
	void sqlFunction() {
		List<String> result = queryFactory
			.select(
				Expressions.stringTemplate("function('replace', {0}, {1}, {2})", // diarect에 있어야함
					member.username, "member", "M")
			).from(member)
			.fetch();

		for (String s : result) {
			System.out.println("s = " + s);
		}
	}

	@Test
	void sqlFunction2() {
		List<String> result = queryFactory
			.select(member.username)
			.from(member)
			.where(member.username.eq(member.username.lower())) // 같은기능을함
			// .where(member.username.eq(Expressions.stringTemplate("function('lower', {0})",
			// 	member.username)))
			.fetch();

		for (String s : result) {
			System.out.println("s = " + s);
		}
	}
}
