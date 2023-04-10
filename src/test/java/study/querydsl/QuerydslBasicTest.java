package study.querydsl;

import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.*;

import javax.persistence.EntityManager;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.querydsl.jpa.impl.JPAQueryFactory;

import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

	@Autowired
	EntityManager em;

	@BeforeEach
	void before() {
		Team teamA= new Team("teamA");
		Team teamB= new Team("teamB");
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
		JPAQueryFactory queryFactory = new JPAQueryFactory(em);
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
}
