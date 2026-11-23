
import java.util.Arrays;
import java.util.Scanner;

public class BankersAlgorithmSimulation {

    static Scanner scanner = new Scanner(System.in);

    static int processes;
    static int resources;

    static int[][] allocation;
    static int[][] maximum;
    static int[][] need;

    static int[] total;
    static int[] totalAllocated;
    static int[] available;

    // STEP 1: Initialize the system
    static void initializeSystem() {

        System.out.println("==========================================");
        System.out.println("           BANKER'S ALGORITHM");
        System.out.println("==========================================");

        System.out.println("\n1. Use sample problem");
        System.out.println("2. Enter your own values");

        int choice = readNumber("Choose an option: ", 1, 2);

        if (choice == 1) {

            loadSample();

        } else {

            inputSystem();
        }
    }

    // Sample problem from the presentation
    static void loadSample() {

        processes = 5;
        resources = 3;

        total = new int[] {10, 5, 7};

        allocation = new int[][] {
            {0, 1, 0},
            {2, 0, 0},
            {3, 0, 2},
            {2, 1, 1},
            {0, 0, 2}
        };

        maximum = new int[][] {
            {7, 5, 3},
            {3, 2, 2},
            {9, 0, 2},
            {2, 2, 2},
            {4, 3, 3}
        };

        System.out.println("\nSample problem loaded successfully.");
    }

    // Read a valid integer within a range
    static int readNumber(String message, int min, int max) {

        while (true) {

            System.out.print(message);

            if (scanner.hasNextInt()) {

                int value = scanner.nextInt();

                if (value >= min && value <= max) {

                    return value;
                }

            } else {

                scanner.next();
            }

            System.out.println(
                "Invalid input. Enter a number from "
                + min + " to " + max + "."
            );
        }
    }

    // STEP 2: Input custom data
    static void inputSystem() {

        processes = readNumber(
            "\nEnter number of processes: ", 1, 100
        );

        resources = readNumber(
            "Enter number of resource types: ", 1, 26
        );

        total = new int[resources];

        allocation = new int[processes][resources];
        maximum = new int[processes][resources];

        System.out.println("\n--- TOTAL RESOURCES ---");

        for (int j = 0; j < resources; j++) {

            total[j] = readNumber(
                "Total Resource " + resourceName(j) + ": ",
                0,
                1000000
            );
        }

        System.out.println("\n--- ALLOCATION MATRIX ---");

        for (int i = 0; i < processes; i++) {

            System.out.println("\nProcess P" + i);

            for (int j = 0; j < resources; j++) {

                allocation[i][j] = readNumber(
                    "Allocation " + resourceName(j) + ": ",
                    0,
                    1000000
                );
            }
        }

        System.out.println("\n--- MAXIMUM MATRIX ---");

        for (int i = 0; i < processes; i++) {

            System.out.println("\nProcess P" + i);

            for (int j = 0; j < resources; j++) {

                maximum[i][j] = readNumber(
                    "Maximum " + resourceName(j) + ": ",
                    allocation[i][j],
                    1000000
                );
            }
        }
    }

    // Convert resource index to a letter
    static String resourceName(int index) {

        return String.valueOf((char) ('A' + index));
    }

    // STEP 3: Calculate total allocated resources
    static void calculateTotalAllocated() {

        totalAllocated = new int[resources];

        for (int j = 0; j < resources; j++) {

            int sum = 0;

            for (int i = 0; i < processes; i++) {

                sum += allocation[i][j];
            }

            totalAllocated[j] = sum;
        }
    }

    // STEP 4: Calculate Available
    static boolean calculateAvailable() {

        available = new int[resources];

        for (int j = 0; j < resources; j++) {

            available[j] = total[j] - totalAllocated[j];

            if (available[j] < 0) {

                System.out.println(
                    "\nERROR: Allocated Resource "
                    + resourceName(j)
                    + " exceeds Total Resources."
                );

                return false;
            }
        }

        return true;
    }

    // STEP 5: Calculate Need = Maximum - Allocation
    static void calculateNeed() {

        need = new int[processes][resources];

        for (int i = 0; i < processes; i++) {

            for (int j = 0; j < resources; j++) {

                need[i][j] = maximum[i][j] - allocation[i][j];
            }
        }
    }

    // Print a resource vector
    static String vectorToString(int[] vector) {

        return Arrays.toString(vector);
    }

    // STEP 6: Display resource computation
    static void displayResourceComputation() {

        System.out.println("\n==========================================");
        System.out.println("       TOTAL RESOURCE COMPUTATION");
        System.out.println("==========================================");

        System.out.printf(
            "%-12s %-12s %-12s %-12s%n",
            "Resource",
            "Total",
            "Allocated",
            "Available"
        );

        for (int j = 0; j < resources; j++) {

            System.out.printf(
                "%-12s %-12d %-12d %-12d%n",
                resourceName(j),
                total[j],
                totalAllocated[j],
                available[j]
            );
        }

        System.out.println("\nFormula:");
        System.out.println("Available = Total - Allocated");

        for (int j = 0; j < resources; j++) {

            System.out.println(
                resourceName(j) + ": "
                + total[j] + " - "
                + totalAllocated[j] + " = "
                + available[j]
            );
        }
    }

    // STEP 7: Display Allocation, Maximum, and Need
    static void displayMatrices() {

        System.out.println("\n==========================================");
        System.out.println("              SYSTEM MATRICES");
        System.out.println("==========================================");

        System.out.printf(
            "%-10s %-20s %-20s %-20s%n",
            "Process",
            "Allocation",
            "Maximum",
            "Need"
        );

        for (int i = 0; i < processes; i++) {

            System.out.printf(
                "%-10s %-20s %-20s %-20s%n",
                "P" + i,
                vectorToString(allocation[i]),
                vectorToString(maximum[i]),
                vectorToString(need[i])
            );
        }

        System.out.println(
            "\nAvailable = " + vectorToString(available)
        );
    }

    // STEP 8: Banker's Safety Algorithm
    static void safetyAlgorithm() {

        int[] work = available.clone();

        boolean[] finish = new boolean[processes];

        int[] safeSequence = new int[processes];

        int count = 0;

        System.out.println("\n==========================================");
        System.out.println("              SAFETY ALGORITHM");
        System.out.println("==========================================");

        System.out.println("\nInitial Work = "
            + vectorToString(work));

        System.out.println("Initially, all Finish values are false.");

        while (count < processes) {

            boolean found = false;

            for (int i = 0; i < processes; i++) {

                if (finish[i]) {

                    continue;
                }

                boolean canExecute = true;

                // Check if Need <= Work
                for (int j = 0; j < resources; j++) {

                    if (need[i][j] > work[j]) {

                        canExecute = false;
                        break;
                    }
                }

                if (canExecute) {

                    System.out.println("\n----------------------------------");
                    System.out.println("Iteration " + (count + 1));
                    System.out.println("----------------------------------");

                    System.out.println("Selected Process: P" + i);

                    System.out.println(
                        "Work Before: "
                        + vectorToString(work)
                    );

                    System.out.println(
                        "Need of P" + i + ": "
                        + vectorToString(need[i])
                    );

                    System.out.println("Need <= Work: TRUE");

                    System.out.println(
                        "Allocation Released: "
                        + vectorToString(allocation[i])
                    );

                    // Work = Work + Allocation
                    for (int j = 0; j < resources; j++) {

                        work[j] += allocation[i][j];
                    }

                    System.out.println(
                        "Work After: "
                        + vectorToString(work)
                    );

                    finish[i] = true;

                    safeSequence[count] = i;

                    count++;

                    found = true;

                    // Restart checking from P0
                    break;
                }
            }

            if (!found) {

                System.out.println(
                    "\nNo unfinished process satisfies Need <= Work."
                );

                break;
            }
        }

        // STEP 9: Final Safety Verification
        System.out.println("\n==========================================");
        System.out.println("          FINAL SAFETY VERIFICATION");
        System.out.println("==========================================");

        for (int i = 0; i < processes; i++) {

            System.out.println(
                "P" + i + " Finish = " + finish[i]
            );
        }

        if (count == processes) {

            System.out.println("\nSYSTEM IS IN A SAFE STATE.");

            System.out.print("Safe Sequence: ");

            for (int i = 0; i < processes; i++) {

                System.out.print("P" + safeSequence[i]);

                if (i < processes - 1) {

                    System.out.print(" -> ");
                }
            }

            System.out.println();

        } else {

            System.out.println("\nSYSTEM IS IN AN UNSAFE STATE.");

            System.out.println("No complete safe sequence exists.");

            System.out.print("Unfinished Processes: ");

            for (int i = 0; i < processes; i++) {

                if (!finish[i]) {

                    System.out.print("P" + i + " ");
                }
            }

            System.out.println();
        }
    }

    // MAIN METHOD
    public static void main(String[] args) {

        initializeSystem();

        calculateTotalAllocated();

        if (!calculateAvailable()) {

            System.out.println("Program stopped: Invalid resources.");

            scanner.close();

            return;
        }

        calculateNeed();

        displayResourceComputation();

        displayMatrices();

        safetyAlgorithm();

        scanner.close();
    }
}
