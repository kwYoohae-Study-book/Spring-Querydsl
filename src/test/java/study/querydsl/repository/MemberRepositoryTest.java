package study.querydsl.repository;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

@Transactional
@SpringBootTest
class MemberRepositoryTest {


	@Autowired
	EntityManager em;

	@Autowired
	MemberRepository memberRepository;

	@Test
	void basicTest() {
		Member member = new Member("member1", 10);
		memberRepository.save(member);

		Member findMember = memberRepository.findById(member.getId()).get();
		assertThat(findMember).isEqualTo(member);

		List<Member> result1 = memberRepository.findAll();
		assertThat(result1).containsExactly(member);

		List<Member> result2 = memberRepository.findByUsername("member1");
		assertThat(result2).containsExactly(member);
	}

	@Test
	void searchTest() {
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

		// 기본조건 및 페이징 쿼리가 존재해야함, limit이라도 줘야함 -> 모든 조건이 없는경우 모든 데이터를 불러오기 때문
		MemberSearchCondition condition = new MemberSearchCondition();
		condition.setAgeGoe(20);
		condition.setAgeLoe(40);
		condition.setTeamName("teamB");

		List<MemberTeamDto> result = memberRepository.search(condition);
		assertThat(result).extracting("username").containsExactly("member3","member4");
	}

	@Test
	void searchPageSimple() {
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

		// 기본조건 및 페이징 쿼리가 존재해야함, limit이라도 줘야함 -> 모든 조건이 없는경우 모든 데이터를 불러오기 때문
		MemberSearchCondition condition = new MemberSearchCondition();
		PageRequest pageRequest = PageRequest.of(0, 3);

		Page<MemberTeamDto> result = memberRepository.searchPageSimple(condition, pageRequest);
		assertThat(result).hasSize(3);
		assertThat(result.getContent()).extracting("username").containsExactly("member1", "member2", "member3");
	}
	
	@Test
	void querydslPredicateExecutorTest() {
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

		// service나 다른계층에 있는 기술에 Repository기술을 사용을 해야한다는 단점존재
		QMember member = QMember.member;
		Iterable<Member> result = memberRepository.findAll(
			member.age.between(10, 40)
				.and(member.username.eq("member1")));

		for (Member findMember : result) {
			System.out.println("member1 = " + findMember);
			
		}
	}

}