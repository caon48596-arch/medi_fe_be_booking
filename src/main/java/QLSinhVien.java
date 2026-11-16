
import java.util.ArrayList;

public class QLSinhVien {

    private ArrayList<SInhVienChinhQuy> DanhSachSV = new ArrayList<SInhVienChinhQuy>();

    public void ThemSV(String masv, String hoten, String khoa, double diemtb, double diemrenluyen, double sotinchi) {
        SInhVienChinhQuy SV = new SInhVienChinhQuy(masv, hoten, khoa, diemtb, diemrenluyen, sotinchi);
        DanhSachSV.add(SV);
    }

    public void HienThi() {
        if (this.DanhSachSV.isEmpty()) {
            System.out.println("khogn co danh sach");
        } else {
            this.DanhSachSV.forEach(sv -> System.out.println(sv));
        }
    }
    public void XoaTheoMa(String masv){
        for(int i = 0; i < DanhSachSV.size(); i++){
            if(this.DanhSachSV.get(i).getmasv().equalsIgnoreCase(masv)){
                this.DanhSachSV.remove(i);
                System.out.println("da xoa thanh cong");
                return;
            }
        }
        System.out.println("xoa khong thanh cong");
    }
    public void Timtheokhoa(String khoa){
        for(SInhVienChinhQuy sv : DanhSachSV){
            if(sv.getkhoa().equalsIgnoreCase(khoa)){
                System.out.println(sv.toString());
                return;
            }
        }
        System.out.println("khhong tim thay");
    }
    public void Suatheoma(String masv, String hoten, String khoa, double diemtb, double diemrenluyen, double sotinchi){
        for(int i = 0; i< DanhSachSV.size();i++){
            if(this.DanhSachSV.get(i).getmasv().equalsIgnoreCase(masv)){
                this.DanhSachSV.remove(i);
                SInhVienChinhQuy sv = new SInhVienChinhQuy(masv, hoten, khoa, diemtb, diemrenluyen, sotinchi);
                DanhSachSV.add(i, sv);
            }
        }
    }
}
