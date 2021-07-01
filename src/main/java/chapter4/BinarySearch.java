package chapter4;

public class BinarySearch {

    public static void main(String[] args) {
        int[] arrays = {1, 2, 3, 4, 5, 6, 7, 8, 9};

        System.out.println(binarySearch(arrays,  7));

    }

    static int binarySearch(int [] arrays, int data) {

        int length = arrays.length;

        if (length == 0) return -1;

        return digui(arrays, 0, length, data);


    }

    static int digui(int [] arrays, int start, int end, int data) {
        int size = end - start;
        if (size == 0) return -1;

        int index = 0;
        if (size % 2 == 0) {
            index = (size/2 + 1) + start;
        } else {
            index = (size / 2) + start;
        }
        if (index > arrays.length-1) return -1;
            if (arrays[index] == data) {
                return index;
            } else if (arrays[index] > data) {
                start = start;
                end = index;
                return digui(arrays, start, end, data);
            } else if (arrays[index] < data) {
                start = index;
                end = end;
                return digui(arrays, start, end, data);
            }
        return -1;
    }
}
