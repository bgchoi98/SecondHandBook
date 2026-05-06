package project.address;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import project.member.MemberVO;
import project.util.Const;

import javax.servlet.http.HttpSession;
import java.util.List;

@Controller
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/profile/address")
public class AddressController {

    private final AddressService addressService;

    @GetMapping("/list")
    @ResponseBody
    public List<AddressVO> getAddressList(HttpSession sess) {
        MemberVO user = (MemberVO) sess.getAttribute(Const.SESSION);
        return addressService.getAddressList(user.getMember_seq());
    }

    @PostMapping("/add")
    @ResponseBody
    public String addAddress(AddressVO vo, HttpSession sess) {
        MemberVO user = (MemberVO) sess.getAttribute(Const.SESSION);

        if (vo.getPost_no() == null || vo.getPost_no().trim().isEmpty()) {
            return "fail_post_no_required"; // 우편번호 필수 입력
        }
        // 우편번호 형식 검사
        if (!vo.getPost_no().matches("\\d{5}")) {  // 예: 5자리 숫자만 허용
            return "fail_invalid_post_no";  // 우편번호가 유효하지 않음을 알리는 msg
        }

        int currentCount = addressService.getAddressCount(user.getMember_seq());
        if (currentCount >= 5) {
            return "count_limit"; // 제한 초과 시 별도의 메시지를 리턴
        }
        vo.setMember_seq(user.getMember_seq());
        // int 타입은 체크 안 하면 0이 되므로 별도 처리 불필요

        boolean result = addressService.addAddress(vo);
        return result ? "success" : "fail";
    }

    // [추가] 수정 요청
    @PostMapping("/update")
    @ResponseBody
    public String updateAddress(AddressVO vo, HttpSession sess) {
        MemberVO user = (MemberVO) sess.getAttribute(Const.SESSION);

        // 우편번호가 비어있는지 또는 유효하지 않은 경우 체크
        if (vo.getPost_no() == null || vo.getPost_no().trim().isEmpty()) {
            return "fail_post_no_required";  // 우편번호가 필수임을 알림
        }

        // 우편번호 형식 검사
        if (!vo.getPost_no().matches("\\d{5}")) {  // 예: 5자리 숫자만 허용
            return "fail_invalid_post_no";  // 우편번호가 유효하지 않음을 알리는 msg
        }

        vo.setMember_seq(user.getMember_seq());

        boolean result = addressService.updateAddress(vo);
        return result ? "success" : "fail";
    }

    @PostMapping("/delete")
    @ResponseBody
    public String deleteAddress(@RequestParam long addr_seq) {
        boolean result = addressService.deleteAddress(addr_seq);
        return result ? "success" : "fail";
    }

    @PostMapping("/setDefault")
    @ResponseBody
    public String setDefaultAddress(@RequestParam long addr_seq, HttpSession sess) {
        MemberVO user = (MemberVO) sess.getAttribute(Const.SESSION);

        boolean result = addressService.setMyDefaultAddress(user.getMember_seq(), addr_seq);
        return result ? "success" : "fail";
    }
}
