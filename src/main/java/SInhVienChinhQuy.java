
public class SInhVienChinhQuy extends SinhVien {

    protected double diemrenluyen;
    protected double sotinhchi;

    public SInhVienChinhQuy(String masv, String hoten, String khoa, double diemtb, double diemrenluyen, double sotinchi) {
        super(masv, hoten, khoa, diemtb);
        this.diemrenluyen = diemrenluyen;
        this.sotinhchi = sotinchi;
    }

    public double getdiemrenluyen() {
        return diemrenluyen;
    }

    public void setdiemrenluyen(double diemrenluyen) {
        this.diemrenluyen = diemrenluyen;
    }

    public double getsotinhchi() {
        return sotinhchi;
    }

    public void setsotinhchi(double sotinhchi) {
        this.sotinhchi = sotinhchi;
    }

    public String xeploai() {
        if (diemTB >= 8.5 && diemrenluyen >= 80) {
            return "xuat sac";
        } else if (diemTB >= 7.0 && diemrenluyen >= 65) {
            return "gioi";
        } else {
            return "kha";
        }
    }

    public double hocbong() {
        if (xeploai().equals("xuat sac")) {
            return 500000000;
        } else if (xeploai().equals("gioi")) {
            return 3000000;
        } else {
            return 0.0;
        }

    }

    public double hotrotinhchi() {
        return sotinhchi * 50000;
    }

    public double tongtien() {
        return hocbong() + hotrotinhchi();
    }

    @Override
    public String toString() {
        return super.toString() + xeploai() + hocbong() + hotrotinhchi()  +tongtien() ;
    }
}

}
