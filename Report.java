import java.io.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.Map.Entry;

class Report {
  public static TreeMap<Long, HashMap<String, Integer>> processUrlHits(String filename) {
    // Use TreeMap for date time ordering
    TreeMap<Long, HashMap<String, Integer>> result = new TreeMap<>();
    int errorCount = 0;
    Scanner in;
    
    try {
      File file = new File(filename);
      in = new Scanner(file);
    }
    catch (FileNotFoundException e) {
      System.out.printf("File not found: %s%n", e);
      return result;
    }
    
    // Read file content line by line to prevent running out of memory
    while (in.hasNext()) {
      String line = in.nextLine();
      String[] tokens = line.split("\\|");
      if (tokens.length != 2) {
        errorCount++;
        continue;
      }
      // Round the time to beginning of date
      Long roundedToDateTime = (Long.parseLong(tokens[0])/86400L)*86400L;
      HashMap<String, Integer> urlMap;
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

  public static void generateReport(TreeMap<Long, HashMap<String, Integer>> allUrlHits) {
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
      for (Long date : allUrlHits.keySet()) {
        LocalDateTime dateTime = LocalDateTime.ofEpochSecond(date, 0, ZoneOffset.UTC);
        // Print date for report  
        System.out.printf("%s GMT%n", dateTime.toLocalDate().format(formatter));

        HashMap<String, Integer> dailyUrlHits = allUrlHits.get(date);
        // Sort by value with descending order
        List<Entry<String, Integer>> sortedList = new ArrayList<>(dailyUrlHits.entrySet());
        sortedList.sort(Collections.reverseOrder(Entry.comparingByValue()));

        // Print url and hit count for report
        for (Entry<String, Integer> e: sortedList) {
          System.out.printf("%s %d%n", e.getKey(), e.getValue());
        }
      }
  }

  // NOTE:
  // Time complexity of sorting is m*log(m), m<=n, m is the maximum number of URLs to be sorted in one day
  // Time complexity of TreeMap get and put functions is n*log(n). So the most signficant time complexity shall be n*log(n)
  // Memory complexity is O(n) due to HashMap memory usage
  
  public static void main(String[] args) {
    if (args.length < 1) {
      System.out.printf("Please specify the file name to proceed%n");
      return;
    }
    // TODO: Add unit tests for postive and negative test cases
    TreeMap<Long, HashMap<String, Integer>> allUrlHits = processUrlHits(args[0]);
    generateReport(allUrlHits);
  } 
}
