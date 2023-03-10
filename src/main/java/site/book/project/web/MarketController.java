package site.book.project.web;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import site.book.project.domain.Book;
import site.book.project.domain.Post;
import site.book.project.domain.UsedBook;
import site.book.project.domain.UsedBookImage;
import site.book.project.domain.UsedBookPost;
import site.book.project.domain.UsedBookWish;
import site.book.project.domain.User;
import site.book.project.dto.MarketCreateDto;
import site.book.project.dto.UserSecurityDto;
import site.book.project.repository.BookRepository;
import site.book.project.repository.PostRepository;
import site.book.project.repository.SearchRepository;
import site.book.project.repository.UsedBookImageRepository;
import site.book.project.repository.UsedBookPostRepository;
import site.book.project.repository.UsedBookRepository;
import site.book.project.repository.UsedBookWishRepository;
import site.book.project.repository.UserRepository;
import site.book.project.service.PostService;
import site.book.project.service.BookService;
import site.book.project.service.UsedBookService;
import site.book.project.service.UserService;

@Slf4j
@Controller
@RequestMapping("/market")
@RequiredArgsConstructor
public class MarketController {
    
    private final SearchRepository searchRepository;
    private final UsedBookService usedBookService;
    private final BookRepository bookRepository;
    private final UsedBookRepository usedBookRepository;
    private final UsedBookPostRepository usedBookPostRepository;
    private final UserRepository userRepository;
    private final UsedBookWishRepository usedBookWishRepository;
    private final UsedBookImageRepository usedBookImageRepository;
    private final PostService postService;
    private final PostRepository postRepository;
    private final UserService userService;
    private final BookService bookService;
    
  
    
    
    
    @GetMapping("/main") // /market/main ???????????? ?????? ????????? ??????
    public void main(@AuthenticationPrincipal UserSecurityDto userDto,String orderSlt,   Model model) {
//        List<UsedBook> usedBookList = usedBookRepository.findByOrderByModifiedTimeDesc();
        
        List<UsedBook> usedBookList = new ArrayList<>(); // ????????? ?????? ?????????
        List<UsedBookPost> usedBookPost = new ArrayList<>(); // ??????????????? ?????????
        
        // ???????????? ????????? ??????? 
        if(orderSlt==null || orderSlt.equals("?????????")) {
            List<UsedBook> storageChk = usedBookRepository.findByOrderByCreatedTimeDesc();            
            for (UsedBook u : storageChk) {
                UsedBookPost post = usedBookPostRepository.findByUsedBookId(u.getId());
                if (post.getStorage() == 1) {
                    usedBookList.add(u);
                } else {
                    usedBookPost.add(post);                    
                }
            }
        }else if(orderSlt.equals("?????????")) {
            List<UsedBook> storageChk = usedBookRepository.findByOrderByHitsDesc();
            for (UsedBook u : storageChk) {
                UsedBookPost post = usedBookPostRepository.findByUsedBookId(u.getId());
                if (post.getStorage() == 1) {
                    usedBookList.add(u);
                } else {
                    usedBookPost.add(post);
                }
            }
        }
        
        List<MarketCreateDto> list = mainList(usedBookList);

        if(userDto != null) {
            model.addAttribute("userId", userDto.getId());       
        }
        
        
        model.addAttribute("list", list);
        model.addAttribute("orderSlt", orderSlt);
        model.addAttribute("usedBookPost", usedBookPost);
        
    }

    @GetMapping("/create") // /market/create ??????????????? ?????? ????????? ??????
    public void create(@AuthenticationPrincipal UserSecurityDto userDto, Model model) {
       
    }
    
    @PostMapping("/create")
    public String createPost( @AuthenticationPrincipal UserSecurityDto userDto, MarketCreateDto dto,
    							Integer usedBookId) {
    	// ??? ????????? ???????????? ??????????????? ?????????
        
        List<String> defaultImg = new ArrayList<>();
        defaultImg.add("booque_logo.jpg");
        
        if(dto.getFileNames() != null) {
            usedBookService.createImg(usedBookId, dto.getFileNames());
            log.info("?????? ???????????????");   
        } else {
            usedBookService.createImg(usedBookId, defaultImg);
        }
        log.info("?????? ???????????????");   
        
        dto.setUserId(userDto.getId());
        dto.setStorage(1); // storage ?????? 1(??????)??? ?????? => ????????? ?????? 0(????????????)
        
    	usedBookService.create( usedBookId, dto );
    	
    	return "redirect:/market/detail?usedBookId="+usedBookId;
    }
    
    
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/storage") // ???????????? -> ?????????????????? ???????????? ??? ????????? ???????????? ?????? ????????? ???!
    public void storage(@AuthenticationPrincipal UserSecurityDto userDto, Model model) {
        // (1) ????????? ????????? ???????????? ?????? ?????? -> userid??? ????????? ??? ????????????(????????????) -> [0]?????? ??? ?????? -> marketcreatedto ???????????? ????????? ??????????
        List<UsedBook> usedBookList = usedBookRepository.findByUserIdOrderByModifiedTimeDesc(userDto.getId());
        
        List<UsedBookPost> usedBookPost = new ArrayList<>(); // storage??? 0??? ????????? ????????? ?????????
        
        for (UsedBook u : usedBookList) { // pk??? UsedBookPost?????? 0??? ?????? ?????? -> ?????? ????????? ?????? ?????? ???
            UsedBookPost post = usedBookPostRepository.findByUsedBookId(u.getId());
            if (post.getStorage() == 0) {
                usedBookPost.add(post);
            }
        }
        
        // usedBookPost[0]??? ?????? ?????????
        UsedBook usedBook = usedBookRepository.findById(usedBookPost.get(0).getUsedBookId()).get();
        Book book = bookRepository.findById(usedBook.getBookId()).get();
        MarketCreateDto dto = MarketCreateDto.builder()
                .usedBookId(usedBook.getId()).bookTitle(book.getBookName()).price(usedBook.getPrice()).location(usedBook.getLocation())
                .level(usedBook.getBookLevel()).title(usedBook.getTitle()).contents(usedBookPost.get(0).getContent())
                .build();
        
        List<UsedBookImage> imgList = usedBookImageRepository.findByUsedBookId(usedBook.getId());
        
        model.addAttribute("imgList", imgList);
        
        model.addAttribute("dto", dto);    
        model.addAttribute("book", book);
        model.addAttribute("usedBook", usedBook);
    }
    
    @PostMapping("/storage") // ???????????? ?????? ??? ???????????? ?????? ???????????? ??????
    public String storage(@AuthenticationPrincipal UserSecurityDto userDto, MarketCreateDto dto, Integer usedBookId) {
        List<String> defaultImg = new ArrayList<>();
        defaultImg.add("booque_logo.jpg");
        
        if(dto.getFileNames()!= null) {
            usedBookService.createImg(usedBookId, dto.getFileNames());
        } else {
            usedBookService.createImg(usedBookId, defaultImg);
        }
        
        log.info("????????????????? ??? ?? {}", dto.getFileNames());
        dto.setUserId(userDto.getId());
        dto.setStorage(0);
        usedBookService.create(usedBookId, dto);
        
        return "redirect:/market/main";
    }
        
    @GetMapping("/detail") // /market/detail ??????????????? ?????? ????????? ??????
    public void detail(@AuthenticationPrincipal UserSecurityDto userDto ,Integer usedBookId, Model model) {
        // ??? ?????? ????????????(bookId) -> postId??? bookId ??????
        // ????????? ?????? ????????????
        // ??????????????? & ?????? & ???????????? & ?????? & ?????? & ???????????? & ????????? & ?????????
        UsedBook usedBook = usedBookRepository.findById(usedBookId).get(); 
        log.info("usedBookId={}", usedBookId);
        UsedBookPost usedBookPost = usedBookPostRepository.findByUsedBookId(usedBookId);
        User user = userRepository.findById(usedBook.getUserId()).get(); // ???????????? ??????
        log.info("id13={}", usedBook.getUserId());
        Book book = bookRepository.findById(usedBook.getBookId()).get();
        
        double bookPrice = book.getPrices();
        double usedPrice = usedBook.getPrice();
        
        double sale =  (1-usedPrice/bookPrice)*100;
        
        UsedBookWish wish = null;
        // ????????? ??? ????????? ????????? ?????? ????????? ?????? ?????? ??? ??????!
        // userId, usedBookId??? ?????????
        if(userDto != null) {
            wish = usedBookWishRepository.findByUserIdAndUsedBookId(userDto.getId(), usedBookId);
        }

        // (??????) ????????? ????????? -> ?????? 1??? + ????????? ?????????
        List<UsedBookImage> imgListAll = usedBookImageRepository.findByUsedBookId(usedBookId);
        UsedBookImage firstImg = imgListAll.get(0); // ??????(????????? ????????? ?????????)
        // imgList.remove(0);
        
        List<UsedBookImage> imgList = new ArrayList<>(); // ????????? ????????? ????????? ????????? ?????????
        for (int i = 1; i < imgListAll.size(); i++) {
            imgList.add(imgListAll.get(i));
        }
        
        // (??????) ?????? ??? ?????? ???????????? ??????
        List<UsedBook> otherUsedBookList = usedBookService.readOtherUsedBook(usedBook.getBookId());
        List<MarketCreateDto> otherUsedBookList2 = mainList(otherUsedBookList);
        List<MarketCreateDto> otherUsedBookListFinal2 = new ArrayList<>();

        for (MarketCreateDto m : otherUsedBookList2) {
            if(usedBookId != m.getUsedBookId()) {
                otherUsedBookListFinal2.add(m);
            }
        }
        
        // (??????) ???????????? ?????? -> ?????? ?????? ?????? ?????? + ?????? ?????? = ??? 2??? ????????????
        List<Post> userPostList = postRepository.findByUserIdOrderByCreatedTime(user.getId()); // ????????? ????????? ???
        
        Post thisBookPost = null;
        Post latestPost = null;
        
        if (userPostList != null) {
            for (Post p : userPostList) {
                if (p.getBook().getBookId() == book.getBookId()) {
                    thisBookPost = p;
                    log.info("?????? ????????? ?????? 1 = {}", thisBookPost);
                    break;
                }
            }
            
            for (Post p : userPostList) {
                if (p.getBook().getBookId() != book.getBookId()) {
                    latestPost = p;
                    log.info("?????? ????????? ?????? 2 = {}", latestPost);
                    break;
                }
            }
        } else {
            thisBookPost = null;
            latestPost = null;
            log.info("?????? ????????? ?????? 3 = {}, {}", thisBookPost, latestPost);
        }
        
        model.addAttribute("thisBookPost", thisBookPost);
        model.addAttribute("latestPost", latestPost);
        model.addAttribute("firstImg", firstImg);
        model.addAttribute("imgList", imgList);
        model.addAttribute("sale", sale);
        model.addAttribute("wish", wish);
        model.addAttribute("book", book);
        model.addAttribute("user", user); 
        model.addAttribute("usedBookPost", usedBookPost);
        model.addAttribute("usedBook", usedBook);
        model.addAttribute("otherUsedBookListFinal2", otherUsedBookListFinal2);
        
    }
    
    // (??????) ????????? ??????
    @GetMapping("/usedBookHitCount")
        public void usedBookHitCount(Integer usedBookId, HttpServletRequest request, HttpServletResponse response) {
            usedBookService.updateHits(usedBookId, request, response);
        }
    
    
    
    

    
    /**
     * UsedBook ???????????? userId, bookId ?????? ????????????
     * @param u user ??????
     * @param bookId  ????????? ?????? PK
     * @return  Map????????? ?????? Book??? usedBookId(PK)??? ??????
     */
    @GetMapping("/createUsed")
    @ResponseBody
    public Map<String, Object>  createUsedBook(@AuthenticationPrincipal UserSecurityDto u, Integer bookId) {
        // ??????
        
        Map<String, Object> maps = new HashMap<>();
        
        Integer usedBookId = usedBookService.create(bookId, u.getId());
        Book book = bookRepository.findById(bookId).get();
        
        maps.put("book", book);
        maps.put("usedBookId", usedBookId);
       return maps; 
    }
    

    /**
     * 
     * @param userId ?????????????????? ????????? user 
     * @param model
     */
//    @GetMapping("/mypage") // /market/mypage ??????????????????&??????????????? ??????
//    public void mypage(Integer userId, @AuthenticationPrincipal UserSecurityDto dto, Model model) {
//        
////        User user = new User(); // ????????? ????????? ?????? ??????
//        Integer usedBookSoldoutCount = null; // ??????????????? ???
//        List<UsedBook> usedBookAll = new ArrayList<>(); // ???????????? ?????? ????????? ?????? ?????????
//        List<UsedBook> usedBook = new ArrayList<>(); // usedBookAll?????? ???????????? ????????? ?????????(????????? ????????? ??????)
//        Integer postCount = 0;
//                
//        if (dto == null && userId != 0) { // ????????? ?????? ?????? ????????? ??????????????? ??????????????? ??? ???
////            user = userRepository.findById(userId).get();
////            log.info("?????? USER ?????? ={}", user.getName());
//            usedBookAll = usedBookRepository.findByUserId(userId);
//            
//            for (UsedBook u : usedBookAll) {
//                UsedBookPost usedBookPost = usedBookPostRepository.findByUsedBookId(u.getId());
//                if (usedBookPost.getStorage() == 1) {
//                    usedBook.add(u);
//                }
//            }
//            
//            usedBookSoldoutCount = usedBookRepository.countUsedBookSoldoutPost(userId, "????????????").size();
//            postCount = postRepository.findByUserId(userId).size();
//        } else if (dto != null && userId == 0) { // ????????? ????????? ??????????????? ??????????????? ??? ???
////            user = userRepository.findById(dto.getId()).get();
////            log.info("?????? USER ?????? ={}", user.getName());
//
//            usedBookAll = usedBookRepository.findByUserId(dto.getId());
//            
//            for (UsedBook u : usedBookAll) {
//                UsedBookPost usedBookPost = usedBookPostRepository.findByUsedBookId(u.getId());
//                if (usedBookPost.getStorage() == 1) {
//                    usedBook.add(u);
//                }
//            }
//            
//            usedBookSoldoutCount = usedBookRepository.countUsedBookSoldoutPost(dto.getId(), "????????????").size();
//            postCount = postRepository.findByUserId(dto.getId()).size();
//        }
//        
//        
//        List<MarketCreateDto> list = mainList(usedBook); // 
//
//    	
////        Book book = bookRepository.findById(userId2).get();
////        String userNickName = user.getNickName();
//        
////        Integer usedBookSellingCount = usedBookRepository.countUsedBookSellingPost(userId).size();
//        
//        Integer usedBookSellingCount = usedBookSoldoutCount;
//        
////        List<UsedBook> usedBookWishList = usedBookRepository.selectUsedBookIdfromUserId(userId);
////        log.info("usedBookWishList = {}", usedBookWishList); // usedBookId??? ???????????? ??????!
////        List<MarketCreateDto> usedBookList = mainList(usedBookWishList);
////        
//        
////        model.addAttribute("usedBookList", usedBookList);
//        model.addAttribute("list", list);
////    	model.addAttribute("userNickName", userNickName);
////        model.addAttribute("user", user);
//        model.addAttribute("usedBook", usedBook);
////        model.addAttribute("book", book);
//        model.addAttribute("postCount", postCount);
//        model.addAttribute("usedBookSellingCount", usedBookSellingCount);
//        model.addAttribute("usedBookSoldoutCount", usedBookSoldoutCount);
//        
//    }
    
    @GetMapping("/mypage") // /market/mypage ??????????????????&??????????????? ??????
    public void mypage(Integer userId, Model model) {
        
        User user = userRepository.findById(userId).get();
        List<UsedBook> usedBookAll = new ArrayList<>(); // ???????????? ?????? ????????? ?????? ?????????
        List<UsedBook> usedBook = new ArrayList<>(); // usedBookAll?????? ???????????? ????????? ?????????(????????? ????????? ??????)
                
        usedBookAll = usedBookRepository.findByUserId(userId);
            
        for (UsedBook u : usedBookAll) {
            UsedBookPost usedBookPost = usedBookPostRepository.findByUsedBookId(u.getId());
            if (usedBookPost.getStorage() == 1) {
                usedBook.add(u);
            }
        }
        
        List<MarketCreateDto> list = mainList(usedBook); 
        
        List<UsedBookWish> wishList = usedBookWishRepository.findByUserId(userId); // ???????????? ?????? ?????????
        List<UsedBook> wishUsedBook = new ArrayList<>();
        
        for (UsedBookWish u : wishList) {
            UsedBook usedBookCHK = usedBookRepository.findById(u.getUsedBookId()).get();
            if (usedBookCHK.getId() == u.getUsedBookId()) {
                wishUsedBook.add(usedBookCHK);
            }
        }
        List<MarketCreateDto> wishListCHK = mainList(wishUsedBook); // ?????? ????????? ??? ????????? ????????? ??????
        
        // ?????? + ????????? + ???????????? ??????
        Integer postCount = postRepository.findByUserId(userId).size();
        Integer usedBookSoldoutCount = usedBookRepository.countUsedBookSoldoutPost(userId, "????????????").size();
        Integer countAllUsedBook = usedBook.size();
        Integer usedBookSellingCount = countAllUsedBook - usedBookSoldoutCount;
        
        model.addAttribute("wishListCHK", wishListCHK);
        model.addAttribute("user", user);
        model.addAttribute("list", list);
        model.addAttribute("usedBook", usedBook);
        model.addAttribute("postCount", postCount);
        model.addAttribute("usedBookSellingCount", usedBookSellingCount);
        model.addAttribute("usedBookSoldoutCount", usedBookSoldoutCount);
        
    }
    
    @GetMapping("/modify")
    public void modify(Integer usedBookId, Model model) {
        UsedBook usedBook = usedBookRepository.findById(usedBookId).get();
        UsedBookPost usedBookPost = usedBookPostRepository.findByUsedBookId(usedBookId);   
        Book book = bookRepository.findById(usedBook.getBookId()).get();
        User user = userRepository.findById(usedBook.getUserId()).get();
        

        List<UsedBookImage> imgList = usedBookImageRepository.findByUsedBookId(usedBookId);
        
        model.addAttribute("imgList", imgList);
        model.addAttribute("usedBook", usedBook);
        model.addAttribute("usedBookPost", usedBookPost);
        model.addAttribute("book", book);
        model.addAttribute("user", user);
    }
    

    
    
    @PostMapping("/modify")
    public String modify(MarketCreateDto dto, String originLocation) {
        log.info("??????????????? ???????????? dto , {}", dto);
        log.info("?????? ?????? ???????????? ?????? ?????? ????????? ???! {}", originLocation);
        log.info( "{}",dto.getFileNames());
        
        if(dto.getFileNames() != null) {
            usedBookService.createImg(dto.getUsedBookId(), dto.getFileNames());
            
        }
        
        
        dto.setStorage(1);
        
        // ??? ?????? null??? ???.. 
        if(dto.getLocation().equals("")) {
            dto.setLocation(originLocation);
        }
        usedBookService.create(dto.getUsedBookId(), dto);
        
        return "redirect:/market/detail?usedBookId=" + dto.getUsedBookId();
    }
    
    
    @GetMapping("/mainSearch")
    public void mainSearch(@AuthenticationPrincipal UserSecurityDto userDto ,String region, String mainKeyword, Model model,
                            String orderSlt , String status) {
        
        
        List<UsedBook> takeList = usedBookService.searchBookList(region, mainKeyword, orderSlt, status);
        
        List<MarketCreateDto> list = mainList(takeList);
        
        if(userDto != null) {
            model.addAttribute("userNickname", userDto.getNickName());       
        }
        
        // (??????) ????????? ????????? ??? ?????? => ?????? ????????? ????????????? 
        // List<UsedBook> bookTitleList = usedBookService.searchByBookTitle(mainKeyword);
        List<Book> list4 = bookService.searchByBookName(mainKeyword);
    
        
        model.addAttribute("status", status);
        model.addAttribute("orderSlt", orderSlt);
        model.addAttribute("list", list);
        model.addAttribute("region", region);
        model.addAttribute("mainKeyword", mainKeyword);
        model.addAttribute("list4", list4);
        
        
    }
    
    
    
    /**
     * main, ????????? ???????????? ?????????. 
     * @param usedBookList
     * @return
     */
    private List<MarketCreateDto> mainList(List<UsedBook> usedBookList) {
        
        List<MarketCreateDto> list = new ArrayList<>();
        
        for (UsedBook ub : usedBookList) {
                User user = userRepository.findById(ub.getUserId()).get();
                Book book = bookRepository.findById(ub.getBookId()).get();
                List<UsedBookImage> imgList = usedBookImageRepository.findByUsedBookId(ub.getId());
                
                MarketCreateDto dto = MarketCreateDto.builder()
                        .usedBookId(ub.getId())
                        .userId(user.getId()).username(user.getUsername())
                        .userImage(user.getUserImage()).nickName(user.getNickName())
                        .bookTitle(book.getBookName()).price(ub.getPrice())
                        .location(ub.getLocation()).level(ub.getBookLevel()).title(ub.getTitle()).modifiedTime(ub.getModifiedTime()).hits(ub.getHits()).wishCount(ub.getWishCount())
                       .imgUsed(imgList.get(0).getFileName()).status(ub.getStatus())
                        .build();
                list.add(dto);
        
        }
        
        
        return list;
    }
    
   
    
}
