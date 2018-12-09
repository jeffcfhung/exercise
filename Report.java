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
    
    while (in.hasNext()) {
      String line = in.nextLine();
      String[] tokens = line.split("\\|");
      if (tokens.length != 2) {
        errorCount++;
        continue;
      }
      Long roundedToDateTime = (Long.parseLong(tokens[0])/86400L)*86400L;
      HashMap<String, Integer> urlMap;
      if (result.containsKey(roundedToDateTime)) {
        urlMap = result.get(roundedToDateTime);
      }
      else {
        urlMap = new HashMap<String, Integer>();
        result.put(roundedToDateTime, urlMap);
      }

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
        System.out.printf("%s GMT%n", dateTime.toLocalDate().format(formatter));

        HashMap<String, Integer> dailyUrlHits = allUrlHits.get(date);
        // Sort by value with descending order
        List<Entry<String, Integer>> sortedList = new ArrayList<>(dailyUrlHits.entrySet());
        sortedList.sort(Collections.reverseOrder(Entry.comparingByValue()));

        for (Entry<String, Integer> e: sortedList) {
          System.out.printf("%s %d%n", e.getKey(), e.getValue());
        }
      }
  }

  // NOTE:
  // Time complexity is mlog(m), m<=n because of sorting for hits count.
  // memory complexity is O(n)
  
  public static void main(String[] args) {
    if (args.length < 1) {
      System.out.printf("Please specify the file name to proceed%n");
      return;
    }
    // TODO: Add unit tests
    TreeMap<Long, HashMap<String, Integer>> allUrlHits = processUrlHits(args[0]);
    generateReport(allUrlHits);
  } 
}
