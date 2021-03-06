package notice.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import notice.vo.NoticeBean;

import static db.JdbcUtil.*;

public class NoticeDAO {
	// ----------------------------------------------------
	// 싱글톤 패턴을 활용한 NoticeDAO 인스턴스 생성 및 리턴
	private NoticeDAO() {}
	
	private static NoticeDAO instance = new NoticeDAO();

	public static NoticeDAO getInstance() {
		return instance;
	}
	// ----------------------------------------------------
	
	Connection con;

	// 외부로부터 Connection 객체를 전달받아 저장할 setConnection 메서드
	public void setConnection(Connection con) {
		this.con = con;
	}

	// ============ 글 쓰기 ============= 
	public int insertArticle(NoticeBean noticeBean) {
		System.out.println("NoticeDAO - insertArticle()");
		int insertCount = 0; // executeUpdate() 메서드를 통해 글쓰기 작업 수행 결과를 저장할 변수
		
		PreparedStatement pstmt = null;
		
		try {
			// INSERT 구문을 사용하여 전달된 항목들 및 기타 데이터를 notice 테이블에 추가
			String sql = "INSERT INTO notice VALUES (null,?,?,?,?,?,now());";
			
			// Connection 객체로부터 PreparedStatement 객체 가져와서 쿼리 전달
			pstmt = con.prepareStatement(sql);
			// ? 파라미터값 채우기
			pstmt.setString(1, noticeBean.getNickname());
			pstmt.setString(2, noticeBean.getSubject());
			pstmt.setString(3, noticeBean.getContent());
			pstmt.setInt(4, 0); //readcount
			pstmt.setString(5, noticeBean.getFile());
			// 쿼리 실행
			insertCount = pstmt.executeUpdate();
			
		} catch (SQLException e) {
//			e.printStackTrace();
			System.out.println("insertArticle() 에러 : " + e.getMessage());
		} finally {
			close(pstmt);
		}
		
		return insertCount;
	}


	// ============ 글 목록 조회 ============= 
	public int selectListCount() {
		// 총 게시물 수 조회하여 리턴
		System.out.println("NoticeDAO - selectListCount()");
		int listCount = 0;
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		try {
			String sql = "SELECT COUNT(*) FROM notice";
			pstmt = con.prepareStatement(sql);
			rs = pstmt.executeQuery();
			
			if(rs.next()) {
				listCount = rs.getInt(1);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			close(rs);
			close(pstmt);
		}
		
		return listCount;
	}

	public ArrayList<NoticeBean> selectArticleList(int page, int limit) {
		// 게시물 목록 조회 후 리턴
		ArrayList<NoticeBean> articleList = new ArrayList<NoticeBean>();
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		/* 
		 * 전체 게시물 중 원하는 페이지의 게시물 첫번째 row 번호 설정
		 * - 원본 글 번호(board_re_ref) 기준으로 내림차순 정렬
		 * - 글 순서번호(board_re_seq) 기준으로 오름차순 정렬
		 * - 조회할 게시물 갯수 : 첫번째 게시물 위치 ~ limit 수 만큼
		 *   첫번째 게시물 위치 = (현재페이지 - 1) * 10
		 * 
		 * ex) 현재 페이지(page) 가 1 페이지 일 경우 : 게시물 조회 결과의 0번 행부터 10개 가져오기
		 */
		int startRow = (page - 1) * 10; // 첫번째 게시물 행(row) 번호 계산
		
		try {
			String sql = "SELECT * FROM notice ORDER BY num DESC LIMIT ?,?";
			pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, startRow);
            pstmt.setInt(2, limit);
            rs = pstmt.executeQuery();
            
            // ResultSet 객체 내의 모든 레코드를 각각 레코드별로 NoticeBean 에 담아서 ArrayList 객체에 저장
            while(rs.next()) {
                NoticeBean noticeBean = new NoticeBean();
                noticeBean.setNum(rs.getInt("num"));
                noticeBean.setNickname(rs.getString("nickname"));
                noticeBean.setSubject(rs.getString("subject"));
                noticeBean.setContent(rs.getString("content"));
                noticeBean.setReadcount(rs.getInt("readcount"));
                noticeBean.setFile(rs.getString("file"));
                noticeBean.setDate(rs.getDate("date"));
                
                articleList.add(noticeBean);
            }
			
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			close(rs);
			close(pstmt);
		}
		
		return articleList;
	}

	// ============ 게시물 상세 내용 조회 ============
	public NoticeBean selectArticle(int num) {
		// 게시물 상세 내용 조회 후 NoticeBean 객체(article)에 저장
		// SELECT 문을 사용하여 게시물 조회 후 정보를 NoticeBean 객체에 저장
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		NoticeBean article = null;
		
		// 글번호(num)에 해당하는 게시물 정보 조회
		try {
			String sql = "SELECT * FROM notice WHERE num=?";
			pstmt = con.prepareStatement(sql);
			pstmt.setInt(1, num);
			rs = pstmt.executeQuery();
			
			// 게시물이 존재할 경우 NoticeBean 객체에 저장
			if(rs.next()) {
				article = new NoticeBean();
				article.setNum(rs.getInt("num"));
				article.setNickname(rs.getString("nickname"));
				article.setSubject(rs.getString("subject"));
				article.setContent(rs.getString("content"));
				article.setReadcount(rs.getInt("readcount"));
                article.setFile(rs.getString("file"));
                article.setDate(rs.getDate("date"));
			}
			
			
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			close(rs);
			close(pstmt);
		}
		
		return article;
	}

	public int updateReadcount(int num) {
		// 게시물 조회 수 1 증가 후 결과(updateCount) 리턴
		// UPDATE 문을 사용하여 게시물 조회수(readcount) 를 1 증가시킴
		PreparedStatement pstmt = null;
		
		int updateCount = 0;
		
		// num 에 해당하는 readcount 값을 1 증가
		try {
			String sql = "UPDATE notice SET readcount=readcount+1 WHERE num=?";
			pstmt = con.prepareStatement(sql);
			pstmt.setInt(1, num);
			updateCount = pstmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			close(pstmt);
		}
		
		return updateCount;
	}

	
	// ============= 글 수정 ==============
	public boolean isArticleWriter(int num) {
		// 글 수정을 위한 본인 확인
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		boolean isArticleWriter = true;
	
		
		return isArticleWriter;
	}

	public int updateArticle(NoticeBean article) {
		// 글번호(num)에 해당하는 게시물에 전달받은 수정 내용(제목, 내용)을 업데이트
		int updateCount = 0;
		
		PreparedStatement pstmt = null;
		
		try {
			String sql = "UPDATE notice SET subject=?,content=? WHERE num=?";
			pstmt = con.prepareStatement(sql);
			pstmt.setString(1, article.getSubject());
			pstmt.setString(2, article.getContent());
			pstmt.setInt(3, article.getNum());
			
			updateCount = pstmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			close(pstmt);
		}
		
		return updateCount;
	}

	
	// ========== 글 삭제 ===========
	// => 삭제를 위한 패스워드 확인은 글 수정의 isArticleWriter() 메서드 함께 사용
	public int deleteArticle(int num) {
		int deleteCount = 0;
		
		PreparedStatement pstmt = null;
		
		try {
			String sql = "DELETE FROM notice WHERE num=?";
			pstmt = con.prepareStatement(sql);
			pstmt.setInt(1, num);
			deleteCount = pstmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			close(pstmt);
		}
		
		return deleteCount;
	}

	

	
	
}
















