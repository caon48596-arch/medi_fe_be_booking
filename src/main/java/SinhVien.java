
public class SinhVien {

    private String maSV, hoTen, khoa;
    double diemTB;

    public SinhVien() {
    }

    public SinhVien(String masv, String hoten, String khoa, double diemtb) {
        this.maSV = masv;
        this.hoTen = hoten;
        this.khoa = khoa;
        this.diemTB = diemtb;
    }

    public String getmasv() {
        return maSV;
    }

    public void setmasv(String masv) {
        this.maSV = masv;
    }

    public String gethoten() {
        return hoTen;
    }

    public void sethoten(String hoten) {
        this.hoTen = hoten;
    }

    public String getkhoa() {
        return khoa;
    }

    public void setkhoa(String khoa) {
        this.khoa = khoa;
    }

    public double getdiemtb() {
        return diemTB;
    }

    public void setdiemtb(double diemtb) {
        this.diemTB = diemtb;
    }

    @Override
    public String toString() {
        return "masv:" + maSV + "tensv :" + hoTen
                + "khoa:" + khoa + "diemtb:" + diemTB + "\n";
    }

}
