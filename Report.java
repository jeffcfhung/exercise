import java.io.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.Map.Entry;

class Report {
  private static Map<Long, Map<String, Integer>> processUrlHits(String filename) {
    // Use TreeMap for date time ordering
    Map<Long, Map<String, Integer>> result = new TreeMap<>();
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
      Map<String, Integer> urlMap;
      if (result.containsKey(roundedToDateTime)) {
        urlMap = result.get(roundedToDateTime);
      }
      else {
        urlMap = new HashMap<String, Integer>();
        result.put(roundedToDateTime, urlMap);
      }

      // Increase hit count of URL
      String url = tokens[1];
      int count = urlMap.getOrDefault(url, 0);
      urlMap.put(url, count+1);
    }
    in.close();

    if (errorCount > 0) {
      System.out.printf("There are %d unexpected lines%n", errorCount);
    }
    return result;
  }

  private static void sortAllUrlHits(Map<Long, Map<String, Integer>> allUrlHits) {
    for (Long date : allUrlHits.keySet()) {
      Map<String, Integer> dailyUrlHits = allUrlHits.get(date);

      List<Entry<String, Integer>> sortedList = new ArrayList<>(dailyUrlHits.entrySet());
      // Sort by value with descending order
      sortedList.sort(Collections.reverseOrder(Entry.comparingByValue()));
      
      // Use LinkedHashMap to keep the order of sorted array
      Map<String, Integer> sortedDailyUrlHits = new LinkedHashMap<>();
      for (Entry<String, Integer> e: sortedList) {
        sortedDailyUrlHits.put(e.getKey(), e.getValue());
      }
      allUrlHits.put(date, sortedDailyUrlHits);
    }
  }

  private static void generateReport(Map<Long, Map<String, Integer>> allUrlHits) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    for (Long date : allUrlHits.keySet()) {
      LocalDateTime dateTime = LocalDateTime.ofEpochSecond(date, 0, ZoneOffset.UTC);
      // Print date for report  
      System.out.printf("%s GMT%n", dateTime.toLocalDate().format(formatter));

      Map<String, Integer> dailyUrlHits = allUrlHits.get(date);
      // Print URL hits for report
      for (Entry<String, Integer> e: dailyUrlHits.entrySet()) {
        System.out.printf("%s %d%n", e.getKey(), e.getValue());
      }
    }
  }

  // NOTE:
  // Time complexity analysis 
  // Time complexity of Hashmap get/set is O(1)
  // Time complexity of sorting url hit count is m*log(m), m<=n, m is the maximum number of URLs to be sorted in one day
  // Time complexity of TreeMap get and put functions is n*log(p), n is total count of url hits, p is total days which is relatively small.
  // Given n >> m > p, the time complexity of this program is O(n)
  // Memory complexity is O(n) by HashMap memory usage
  
  public static void main(String[] args) {
    if (args.length < 1) {
      System.out.printf("Please specify the file name to proceed%n");
      return;
    }
    // TODO: Add unit tests for postive and negative test cases
    Map<Long, Map<String, Integer>> allUrlHits = processUrlHits(args[0]);
    sortAllUrlHits(allUrlHits);
    generateReport(allUrlHits);
  } 
}
