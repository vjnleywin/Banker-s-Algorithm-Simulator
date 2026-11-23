import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Pure computation class; no Swing code belongs here. */
public final class BankersAlgorithm {
    public static final class Step {
        public final int process;
        public final int[] before, need, released, after;
        Step(int process, int[] before, int[] need, int[] released, int[] after) {
            this.process = process;
            this.before = before.clone();
            this.need = need.clone();
            this.released = released.clone();
            this.after = after.clone();
        }
    }

    public static final class Result {
        public final boolean safe;
        public final int[] available, totalAllocated, sequence;
        public final int[][] need;
        public final boolean[] finish;
        public final List<Step> steps;
        Result(boolean safe, int[] available, int[] totalAllocated, int[][] need,
               boolean[] finish, int[] sequence, List<Step> steps) {
            this.safe = safe;
            this.available = available.clone();
            this.totalAllocated = totalAllocated.clone();
            this.need = copy(need);
            this.finish = finish.clone();
            this.sequence = sequence.clone();
            this.steps = steps;
        }
    }

    public static final class RequestResult {
        public final boolean granted;
        public final String explanation;
        public final Result tentativeResult;
        RequestResult(boolean granted, String explanation, Result tentativeResult) {
            this.granted = granted;
            this.explanation = explanation;
            this.tentativeResult = tentativeResult;
        }
    }

    private BankersAlgorithm() {}

    public static int[][] copy(int[][] matrix) {
        int[][] out = new int[matrix.length][];
        for (int i = 0; i < matrix.length; i++) {
            out[i] = matrix[i].clone();
        }
        return out;
    }

    public static Result analyze(int[] total, int[][] allocation, int[][] maximum) {
        validate(total, allocation, maximum);
        int n = allocation.length;
        int m = total.length;
        int[] allocated = new int[m];
        int[][] need = new int[n][m];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                allocated[j] += allocation[i][j];
                need[i][j] = maximum[i][j] - allocation[i][j];
            }
        }
        int[] available = new int[m];
        for (int j = 0; j < m; j++) {
            if (allocated[j] > total[j]) {
                throw new IllegalArgumentException("Allocated resource " + resourceName(j)
                    + " exceeds the total available in the system.");
            }
            available[j] = total[j] - allocated[j];
        }

        int[] work = available.clone();
        boolean[] finish = new boolean[n];
        List<Integer> order = new ArrayList<>();
        List<Step> steps = new ArrayList<>();

        while (order.size() < n) {
            boolean progressed = false;
            for (int i = 0; i < n; i++) {
                if (finish[i]) continue;
                boolean canFinish = true;
                for (int j = 0; j < m; j++) {
                    if (need[i][j] > work[j]) {
                        canFinish = false;
                        break;
                    }
                }
                if (!canFinish) continue;
                int[] before = work.clone();
                for (int j = 0; j < m; j++) {
                    work[j] += allocation[i][j];
                }
                steps.add(new Step(i, before, need[i], allocation[i], work));
                finish[i] = true;
                order.add(i);
                progressed = true;
                break; // restart at P0 for a reproducible safe sequence
            }
            if (!progressed) break;
        }
        int[] sequence = order.stream().mapToInt(Integer::intValue).toArray();
        return new Result(order.size() == n, available, allocated, need, finish, sequence, steps);
    }

    public static RequestResult checkRequest(int[] total, int[][] allocation,
                                              int[][] maximum, int process, int[] request) {
        Result current = analyze(total, allocation, maximum);
        if (process < 0 || process >= allocation.length) {
            throw new IllegalArgumentException("Invalid process number.");
        }
        if (request == null || request.length != total.length) {
            throw new IllegalArgumentException("Request vector length is incorrect.");
        }
        for (int j = 0; j < total.length; j++) {
            if (request[j] < 0) {
                throw new IllegalArgumentException("Requests cannot be negative.");
            }
            if (request[j] > current.need[process][j]) {
                return new RequestResult(false,
                    "WAIT / INVALID: Request <= Need fails for resource " + resourceName(j) + ".", null);
            }
            if (request[j] > current.available[j]) {
                return new RequestResult(false,
                    "WAIT: Request <= Available fails for resource " + resourceName(j) + ".", null);
            }
        }
        int[][] tentative = copy(allocation);
        for (int j = 0; j < total.length; j++) {
            tentative[process][j] += request[j];
        }
        Result trial = analyze(total, tentative, maximum);
        return new RequestResult(trial.safe,
            trial.safe ? "GRANT: Request <= Need and Available; tentative state is SAFE."
                       : "WAIT: Request fits Need and Available, but tentative state is UNSAFE.", trial);
    }

    private static void validate(int[] total, int[][] allocation, int[][] maximum) {
        if (total == null || total.length == 0 || allocation == null || maximum == null
                || allocation.length == 0 || maximum.length != allocation.length) {
            throw new IllegalArgumentException("Enter valid process and resource dimensions.");
        }
        for (int j = 0; j < total.length; j++) {
            if (total[j] < 0) throw new IllegalArgumentException("Total resources cannot be negative.");
        }
        for (int i = 0; i < allocation.length; i++) {
            if (allocation[i] == null || maximum[i] == null
                    || allocation[i].length != total.length || maximum[i].length != total.length) {
                throw new IllegalArgumentException("Matrix dimensions do not match.");
            }
            for (int j = 0; j < total.length; j++) {
                if (allocation[i][j] < 0 || maximum[i][j] < 0
                        || maximum[i][j] < allocation[i][j]) {
                    throw new IllegalArgumentException("For P" + i + " resource " + resourceName(j)
                        + ": require 0 <= Allocation <= Maximum.");
                }
                if (maximum[i][j] > total[j]) {
                    throw new IllegalArgumentException("Maximum for P" + i + " resource "
                        + resourceName(j) + " exceeds the system total.");
                }
            }
        }
    }

    public static String resourceName(int index) {
        return index < 26 ? String.valueOf((char) ('A' + index)) : "R" + (index + 1);
    }

    public static String vector(int[] values) {
        return Arrays.toString(values);
    }
}
