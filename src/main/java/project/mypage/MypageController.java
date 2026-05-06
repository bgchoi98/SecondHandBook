package project.mypage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import project.address.AddressService;
import project.bookclub.service.BookClubService;
import project.bookclub.vo.BookClubVO;
import project.member.MemberVO;
import project.trade.TradeService;
import project.trade.TradeVO;
import project.util.Const;

import javax.servlet.http.HttpSession;
import java.util.List;

@Controller
@Slf4j
@RequiredArgsConstructor
public class MypageController {

    private final TradeService tradeService;
    private final BookClubService bookClubService; // [추가] 모임 서비스 주입
    private final AddressService addressService;

    @GetMapping("/mypage")
    public String mypage(HttpSession sess, Model model) {
        model.addAttribute("currentTab", "profile");
        return "member/mypage";
    }

    @GetMapping("/mypage/{tab}")
    public String mypageWithTab(@PathVariable String tab,
                                @RequestParam(required = false) String status,
                                HttpSession sess,
                                Model model) {
        model.addAttribute("currentTab", tab);
        model.addAttribute("status", status);
        return "member/mypage";
    }

    @GetMapping("/mypage/tab/{tab}")
    public String loadTab(@PathVariable String tab,
                          @RequestParam(required = false) String status,
                          HttpSession sess,
                          Model model
    ) {
        log.info("=========loadTab 호출됨: tab={}, status={}", tab, status);
        // 로그인 체크
        MemberVO loginSess = (MemberVO) sess.getAttribute(Const.SESSION);
        Long member_seq = loginSess.getMember_seq();
        // 탭별 초기 데이터 로드 (SSR이 필요한 경우 여기서 처리)
        // 현재 대부분 AJAX로 처리하므로 비워두거나 기본값만 설정
        switch (tab) {
            case "profile" :
                log.info("profile 탭로드");
                break;
            case "purchases" :
                log.info("purchases 탭 로드, status={}", status);
                List<TradeVO> purchaseList = tradeService.getPurchaseTrades(member_seq);
                model.addAttribute("purchaseList", purchaseList);
                break;
            case "sales" :
                log.info("sales 탭 로드, status={}", status);
                String salesStatus = status != null ? status : "all";
                List<TradeVO> salesList = tradeService.getSaleTrades(member_seq, salesStatus);
                model.addAttribute("salesList", salesList);
//                model.addAttribute("selectedStatus", salesStatus); // 셀렉트 박스 삭제 요청
                break;
            case "wishlist" :
                break;
            case "groups" :
                // 내 모임은 groups.jsp에서 AJAX로 로딩하므로 여기선 처리 없음
                break;
            case "addresses" :
                break;
            default:
                // 지원하지 않는 탭은 profile로 처리 (redirect 대신 직접 반환)
                log.info("지원하지 않는 탭: {}, profile로 대체", tab);
                return "member/tabs/profile";
        }
        log.info("반환 JSP: member/tabs/{}", tab);
        return "member/tabs/" + tab;
    }

    // ---------------------------------------------------------
    // AJAX 요청 처리 메서드 (JSP의 $.ajax URL과 매핑)
    // ---------------------------------------------------------
    
    // [AJAX] 내 모임 데이터 조회
    @GetMapping("/profile/bookclub/list")
    @ResponseBody
    public List<BookClubVO> getMyBookClubs(HttpSession sess) {
        MemberVO user = (MemberVO) sess.getAttribute(Const.SESSION);

        // BookClubService를 통해 데이터 조회
        return bookClubService.getMyBookClubs(user.getMember_seq());
    }
    // [AJAX] 찜한 상품 목록
    @GetMapping("/profile/wishlist/trade")
    @ResponseBody
    public List<TradeVO> getWishTrades(HttpSession sess) {
        MemberVO user = (MemberVO) sess.getAttribute(Const.SESSION);
        return tradeService.getWishTrades(user.getMember_seq());
    }

    // [AJAX] 찜한 모임 목록
    @GetMapping("/profile/wishlist/bookclub")
    @ResponseBody
    public List<BookClubVO> getWishBookClubs(HttpSession sess) {
        MemberVO user = (MemberVO) sess.getAttribute(Const.SESSION);
        return bookClubService.getWishBookClubs(user.getMember_seq());
    }

}
