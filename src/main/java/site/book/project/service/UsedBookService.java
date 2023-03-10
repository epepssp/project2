package site.book.project.service;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import site.book.project.domain.UsedBook;
import site.book.project.domain.UsedBookImage;
import site.book.project.domain.UsedBookPost;
import site.book.project.domain.UsedBookWish;
import site.book.project.domain.User;
import site.book.project.dto.MarketCreateDto;
import site.book.project.repository.UsedBookImageRepository;
import site.book.project.repository.UsedBookPostRepository;
import site.book.project.repository.UsedBookRepository;
import site.book.project.repository.UsedBookWishRepository;
import site.book.project.repository.UserRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsedBookService {
	// 부끄마켓 글 하나는 세개의 테이블로 되어 있음.
	// service를 하나로 뭉쳐서 만들거임
	
	private final UsedBookRepository usedBookRepository;
	private final UsedBookPostRepository postRepository;
	private final UsedBookImageRepository imgRepository;
	private final UsedBookWishRepository usedBookWishRepository;
	private final UsedBookPostRepository usedBookPostRepository;
	private final UserRepository userRepository;
	private final UsedBookImageRepository usedBookImgRepository;

	
    @Value("${com.example.upload.path}")
    private String uploadPath;
	
	/**(은정)
	 * 책 검색 후 바로 UsedBook테이블에 저장하여 UsedBookPost와 UsedBookImage에 연결할 수 있는
	 * FK를 리턴해줌. 잘하면 임시저장할 수 있지 않을까?
	 * @param bookId 판매할 책의 PK 
	 * @param userId 판매하는 사용자의 PK
	 * @return 생성된 UsedBook의 PK
	 */
	public Integer create(Integer bookId, Integer userId) {
		
		UsedBook usedBook = usedBookRepository.save(UsedBook.builder().userId(userId).bookId(bookId).build());
		
		// (하은) UsedBookPost에도 같이 저장 - 임시저장용
		postRepository.save(UsedBookPost.builder().usedBookId(usedBook.getId()).build());
		
		return usedBook.getId();
	}
	
	/**(은정)
	 * UsedBook는 Transactional를 통한 update
	 * UsedBookContent는 객체 생성 후 save
	 * @param usedBookId UsedBook의 PK
	 * @param dto market/create에서 받은 데이터(사진 DB제외)
	 */
	@Transactional
	public void create(Integer usedBookId, MarketCreateDto dto ) {
		
		// UsedBook 디비에 저장
		UsedBook entity = usedBookRepository.findById(usedBookId).get();
		entity.update(dto);
		
		// UsedBookPost에 저장
		UsedBookPost content = postRepository.findByUsedBookId(usedBookId);
		
		if(content != null) {
		    content.update(dto.getContents(), dto.getStorage());
		}else {
		    postRepository.save(UsedBookPost.builder().usedBookId(usedBookId).content(dto.getContents()).storage(dto.getStorage()).build());
		    
		}
		
		
	}
	
	@Transactional
	public void createImg(Integer usedBookId, List<String> fileNames) {
	    
	    UsedBookImage entity = null;
	    
	    for(String f : fileNames) {
	        String orgFileName = f.split("_")[1];
	        
	        entity = UsedBookImage.builder().usedBookId(usedBookId).fileName(f).filePath(uploadPath+f).origFileName(orgFileName).build();
	        imgRepository.save(entity);
	    }
	    
	    
	    
	}
	
	
	
	
	

	public Integer addUsedBookWish(Integer usedBookId, Integer userId) {
	// 사용자 정보가 있으면 값을 바꾸고, 없으면 만들기 
	    UsedBookWish wish = usedBookWishRepository.findByUserIdAndUsedBookId(userId, usedBookId);
	    Integer result = 0;
	    
	    if(wish == null) {
	        usedBookWishRepository.save(UsedBookWish.builder().usedBookId(usedBookId).userId(userId).build());
	        result = 1;
	    } else {
            usedBookWishRepository.delete(wish);
            result = 0;
        }
	    
	    // 0은 삭제 1은 존재
	    return result;
	    
	}
	
	@Transactional
	public Integer addWishCount(Integer usedBookId) {
	    
	    UsedBook usedBook = usedBookRepository.findById(usedBookId).get();
	    Integer a = usedBook.getWishCount() + 1;
	    usedBook = usedBook.updateWishCount(a);
	    usedBookRepository.save(usedBook);
	    return a;
	}
	
	@Transactional
	public Integer minusWishCount(Integer usedBookId) {
	    UsedBook usedBook = usedBookRepository.findById(usedBookId).get();
	    
	    Integer a = usedBook.getWishCount() - 1;
	    
	    usedBook = usedBook.updateWishCount(a);
	    usedBookRepository.save(usedBook);
	    
	    return a;
	}
	
	

	// (하은) bookId가 동일한 다른 중고책 리스트 만들기
    public List<UsedBook> readOtherUsedBook(Integer bookId) {
        log.info("하은 중고책의 책 정보를 가진 아이디는? = {}", bookId);
        
        // (1) 같은 책의 중고판매글 리스트
        List<UsedBook> otherUsedBookListAll = usedBookRepository.findByBookId(bookId); 
        
        // (2) 임시저장 글 제외한 리스트 재생성
        List<UsedBook> otherUsedBookList = new ArrayList<>();
        
        for (UsedBook u : otherUsedBookListAll) {
            UsedBookPost storageChk = usedBookPostRepository.findByUsedBookId(u.getId());
            if (storageChk.getStorage() != 0) {
                otherUsedBookList.add(u);
            }
        }
        
        return otherUsedBookList;
    }

    // (하은) 중고판매글 조회수
    /**
     * 
     * @param id 조회수를 증가시킬 id(=> UsedBook의 PK)
     * @param request
     * @param response
     * @return
     */
    public void updateHits(Integer usedBookId, HttpServletRequest request, HttpServletResponse response) {
        UsedBook usedBook = usedBookRepository.findById(usedBookId).get();
        //log.info("조회수 확인할 usedBookId = {}", usedBookId);
        
        Cookie oldCookie = null;
        Cookie[] cookies = request.getCookies(); // 현재 쿠키들 배열
                
        if (cookies != null) { // 이미 쿠키들이 있을 때 usedBookView 쿠키가 존재유무 체크
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("usedBookView")) {
                    oldCookie = cookie;
                }
            }
        }
        
        if (oldCookie != null) {
            if (!oldCookie.getValue().contains("[" + usedBookId.toString() + "]")) { // 해당 쿠키가 없을시
                usedBook = usedBook.updateHits();                
                usedBookRepository.save(usedBook);
                
                oldCookie.setValue(oldCookie.getValue() + "[" + usedBookId + "]");
                oldCookie.setPath("/");
                oldCookie.setMaxAge(60 * 30);
                response.addCookie(oldCookie);
            }
        } else {
            usedBook = usedBook.updateHits(); // usedBookView가 없으면 조회수 1 증가             
            usedBookRepository.save(usedBook);
            Cookie newCookie = new Cookie("usedBookView", "[" + usedBookId + "]"); // 쿠키 생성
            newCookie.setPath("/");
            newCookie.setMaxAge(60 * 30);
            response.addCookie(newCookie);
        }
        
    }

    public List<UsedBook> searchBookList(String region, String mainKeyword, String orderSlt, String status) {
        // 지역별, 검색을 한다.
        // 순서를 정할 수 있다.
        // 상태를 선택할 수 있다.
        // 전체 지역에서, 검색어를 최신순으로 구매가능한 것만   함.
        
        List<UsedBook> usedBookList = new ArrayList<>();
        
        if(status.equals("all")) {
            if(orderSlt == null  || orderSlt.equals("최신순")) {
                usedBookList = usedBookRepository.searchM(region, mainKeyword);
            } else if(orderSlt.equals("최저가순")) {
                usedBookList = usedBookRepository.searchPrice(region, mainKeyword);
            } else if(orderSlt.equals("최고가순")) {
                usedBookList = usedBookRepository.searchPriceDesc(region, mainKeyword);
            }
            
        }else {
            if(orderSlt == null  || orderSlt.equals("최신순")) {
                usedBookList = usedBookRepository.searchM2(region, mainKeyword);
            } else if(orderSlt.equals("최저가순")) {
                usedBookList = usedBookRepository.searchPrice2(region, mainKeyword);
            } else if(orderSlt.equals("최고가순")) {
                usedBookList = usedBookRepository.searchPriceDesc2(region, mainKeyword);
            }
            
        }
        
        return usedBookList;
    }

    public UsedBook read(Integer usedBookId) {
        UsedBook usedBook = usedBookRepository.findById(usedBookId).get();
        return usedBook;
    }

   
    
    // (하은) 부끄마켓 찜한 목록 불러오기
    public List<MarketCreateDto> searchWishList(Integer id) {
        log.info("보이시나요 제가???? {}  ", id);
        
        // USEDBOOKWISH에서 목록 찾기
        // 해당 리스트에서 USEDBOOKID로 USEDBOOK 정보 찾기
        List<UsedBookWish> wishList = usedBookWishRepository.findByUserId(id);
        List<MarketCreateDto> usedBookList = new ArrayList<>();
        
        for (UsedBookWish u : wishList) {
            UsedBook usedBook = usedBookRepository.findById(u.getUsedBookId()).get();
            List<UsedBookImage> usedBookImage = usedBookImgRepository.findByUsedBookId(usedBook.getId());
            User seller = userRepository.findById(usedBook.getUserId()).get();
            MarketCreateDto dto = MarketCreateDto.builder().username(seller.getUsername()).bookTitle(usedBook.getBookTitle())
                    .price(usedBook.getPrice()).location(usedBook.getLocation()).title(usedBook.getTitle())
                    .usedBookId(usedBook.getId()).bookId(usedBook.getBookId())
                    .imgUsed(usedBookImage.get(0).getFileName()).build();
            usedBookList.add(dto);
        }
        
        return usedBookList;
    }
    
    
    
    
    
}
