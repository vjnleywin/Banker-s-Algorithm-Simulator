import java.util.Arrays;

public class BankersTests {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        int[] total = {10, 5, 7};
        int[][] allocation = {{0,1,0},{2,0,0},{3,0,2},{2,1,1},{0,0,2}};
        int[][] maximum = {{7,5,3},{3,2,2},{9,0,2},{2,2,2},{4,3,3}};
        BankersAlgorithm.Result safe = BankersAlgorithm.analyze(total, allocation, maximum);
        check(safe.safe, "Test 1 should be safe");
        check(Arrays.equals(safe.available, new int[]{3,3,2}), "Available mismatch");
        check(Arrays.equals(safe.sequence, new int[]{1,3,0,2,4}), "Safe sequence mismatch");
        System.out.println("PASS Test 1: SAFE, Available [3,3,2], P1->P3->P0->P2->P4");

        int[] total2 = {1,1};
        int[][] alloc2 = {{1,0},{0,1}};
        int[][] max2 = {{1,1},{1,1}};
        BankersAlgorithm.Result unsafe = BankersAlgorithm.analyze(total2, alloc2, max2);
        check(!unsafe.safe && unsafe.steps.isEmpty(), "Test 2 should be unsafe");
        System.out.println("PASS Test 2: UNSAFE, no process can complete");

        BankersAlgorithm.RequestResult request = BankersAlgorithm.checkRequest(
            total, allocation, maximum, 1, new int[]{1,0,2});
        check(request.granted, "Test 3 should grant request");
        System.out.println("PASS Test 3: GRANT P1 request [1,0,2]");

        BankersAlgorithm.RequestResult wait = BankersAlgorithm.checkRequest(
            total, allocation, maximum, 4, new int[]{3,3,0});
        check(!wait.granted, "Test 4 should wait");
        System.out.println("PASS Test 4: WAIT P4 request [3,3,0]");
    }
}
