import java.io.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

class Report {
  private static Map<Long, Map<String, Long>> processUrlHits(String filename) {
    Map<Long, Map<String, Long>> result = new HashMap<>();
    int errorCount = 0;
    Scanner in;
    
    try {
      File file = new File(filename);
      in = new Scanner(file);
    }
    catch (FileNotFoundException e) {
      System.out.printf("File not found: %s%n", e.getLocalizedMessage());
      return result;
    }
    
    // Read line by line to prevent loading all file contents to memory at once
    while (in.hasNext()) {
      String line = in.nextLine();
      String[] tokens = line.split("\\|");
      if (tokens.length != 2) {
        errorCount++;
        continue;
      }
      // Round time to beginning of date
      Long roundedToDateTime = (Long.parseLong(tokens[0])/86400L)*86400L;
      Map<String, Long> urlMap;
      if (result.containsKey(roundedToDateTime)) {
        urlMap = result.get(roundedToDateTime);
      }
      else {
        urlMap = new HashMap<String, Long>();
        result.put(roundedToDateTime, urlMap);
      }

      // Increase hit count of URL
      String url = tokens[1];
      Long count = urlMap.getOrDefault(url, 0L);
      urlMap.put(url, count + 1L);
    }
    in.close();

    if (errorCount > 0) {
      System.out.printf("There are %d unexpected lines%n", errorCount);
    }
    return result;
  }

  private static List<Entry<Long, List<Entry<Long, String>>>> radixSortAllUrlHits(
    Map<Long, Map<String, Long>> allUrlHits
  ) {
    List<Entry<Long, Map<String, Long>>> allUrlHitList = new ArrayList<>(allUrlHits.entrySet());
    List<Entry<Long, List<Entry<Long, String>>>> sortedAllUrlHitList = new ArrayList<>();
    // Sort by date time
    radixSort(allUrlHitList);
    
    for (Entry<Long, Map<String, Long>> allUrlHitEntry : allUrlHitList) {
      Map<String, Long> dailyUrlHits = allUrlHitEntry.getValue();
      List<Entry<Long, String>> dailyUrlHitList = new ArrayList<>();
      for (Entry<String, Long> dailyUrlHitEntry: dailyUrlHits.entrySet()) {
        // Swap the key/value so we can do radix sort with same interface
        dailyUrlHitList.add(
          new AbstractMap.SimpleEntry<Long, String>(
            dailyUrlHitEntry.getValue(),
            dailyUrlHitEntry.getKey()
          )
        );
      }
      radixSort(dailyUrlHitList);

      sortedAllUrlHitList.add(
        new AbstractMap.SimpleEntry<Long, List<Entry<Long, String>>>(
          allUrlHitEntry.getKey(),
          dailyUrlHitList
        )
      );
    }
    return sortedAllUrlHitList;
  }

  private static <T> void radixSort(List<Entry<Long, T>> inputArray) {
    final int RADIX = 10;

    List<List<Entry<Long, T>>> buckets = new ArrayList<>();
    for (int i=0; i<RADIX; i++) {
      buckets.add(new ArrayList<Entry<Long, T>>());
    }
    
    Long target, digitBase = 1L;
    boolean isMaxDigitReached = false;
    while (isMaxDigitReached == false) {
      isMaxDigitReached = true;
      for (Entry<Long, T> entry: inputArray) {
        target = entry.getKey() / digitBase;
        // Assign to corresponding bucket
        buckets.get(Math.toIntExact(target%RADIX)).add(entry);
        if (isMaxDigitReached == true && target > 0) {
          isMaxDigitReached = false;
        }
      }

      int i = 0;
      for (int j=0; j<RADIX; j++) {
        // Reassign back to inputArray and keep the partial sorted order
        for (Entry<Long, T> entry: buckets.get(j)) {
          inputArray.set(i++, entry);
        }
        buckets.get(j).clear();
      }
      // Advance to next digit
      digitBase = digitBase * RADIX;
    }
  }

  private static void generateReport(
    List<Entry<Long, List<Entry<Long, String>>>> allUrlHits
  ) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    for (Entry<Long, List<Entry<Long, String>>> allUrlHitEntry: allUrlHits) {
      Long date = allUrlHitEntry.getKey();
      LocalDateTime dateTime = LocalDateTime.ofEpochSecond(date, 0, ZoneOffset.UTC);
      // Print date for report in ascending order by default
      System.out.printf("%s GMT%n", dateTime.toLocalDate().format(formatter));

      List<Entry<Long, String>> dailyUrlHits = allUrlHitEntry.getValue();
      // Print URL hits for report in descending order 
      ListIterator<Entry<Long, String>> li = dailyUrlHits.listIterator(dailyUrlHits.size());
      while (li.hasPrevious()) {
        Entry<Long, String> dailyUrlHitEntry = li.previous();
        System.out.printf("%s %d%n", dailyUrlHitEntry.getValue(), dailyUrlHitEntry.getKey());
      }
    }
  }

  /**
   * NOTE: Time complexity analysis
   * 
   *  * 12/12/2018
   * Question from team: Is there a way for you to optimize the runtime of your solution?
   * 
   * Improved Solution: Radix sort (LSD)
   * Time complexity of Radix sort is O(k(m+n)). k is maximum integer length,
   * n is date count, and m is maximum count of unqiue URLs in one day
   * 
   * As long as k*(m+n)<(n*log(n)+m*log(m)), runtime complexity of Radix sort will be better than 
   * comparision based sorting algorithm because best runtime is O(n*log(n))
   * 
   * 
   * Obsolete Solution: Comparison-based sort (previous version)
   * Time complexity of Hashmap get/set is O(1)
   * 
   * Worst case scenario 1: n is the count of unique URL hits in one day
   * Time complexity of sorting url hit count is O(n*log(n))
   * 
   * Worst case scenario 2: n days and only one URL hit within one day
   * Time complexity of sorting by date is O(n*log(n))
   * 
   * Average case scenario: n days and m unique URL hit counts
   * Time complexity is n*log(n) + m*log(m)
   * 
   * Therefore overall time complexity is O(n*log(n))
   * 
   * Memory complexity is O(n) by HashMap memory usage
   */
  public static void main(String[] args) {
    if (args.length < 1) {
      System.out.printf("Please specify the file name to proceed%n");
      return;
    }
    // TODO: Add unit tests for postive and negative test cases
    Map<Long, Map<String, Long>> allUrlHits = processUrlHits(args[0]);
    List<Entry<Long, List<Entry<Long, String>>>> sortedAllUrlHits = radixSortAllUrlHits(allUrlHits);
    generateReport(sortedAllUrlHits);
  } 
}
