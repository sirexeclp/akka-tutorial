package de.uni_potsdam.hpi;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;


public class SerialAnalyzer {
	
	private static final int maxPassword = 1000000;
	private static final int prefixLength = 5;

	public static void main(String[] args) throws IOException{
		//System.out.println(Files.readAllLines(Paths.get("students.csv")).toArray(new String[0]));
		String [] tmp = Files.readAllLines(Paths.get("students.csv")).toArray(new String [0]);
		new SerialAnalyzer().analyze(Arrays.copyOfRange(tmp, 1, tmp.length));
	}
	static int LCSubStr(String a, String b)  
    { 
		char X[] = a.toCharArray();
		char Y[] = b.toCharArray();
        // Create a table to store lengths of longest common suffixes of 
        // substrings. Note that LCSuff[i][j] contains length of longest 
        // common suffix of X[0..i-1] and Y[0..j-1]. The first row and 
        // first column entries have no logical meaning, they are used only 
        // for simplicity of program 
        int LCStuff[][] = new int[X.length + 1][Y.length + 1]; 
		int result = 0;  // To store length of the longest common substring
		int end=0;
          
        // Following steps build LCSuff[m+1][n+1] in bottom up fashion 
        for (int i = 0; i <= X.length; i++)  
        { 
            for (int j = 0; j <= Y.length; j++)  
            { 
                if (i == 0 || j == 0) 
                    LCStuff[i][j] = 0; 
                else if (X[i - 1] == Y[j - 1]) 
                { 
                    LCStuff[i][j] = LCStuff[i - 1][j - 1] + 1; 
					
					if(LCStuff[i][j] > result)
					{
						end = i-1;
					}
					result = Integer.max(result, LCStuff[i][j]);	
                }  
                else
                    LCStuff[i][j] = 0; 
            } 
		} 
		// System.out.println(end);
		// System.out.println(result);
		// System.out.println(a.length());
		return result;
        //return a.substring(end-result+1,end+1); 
    } 
	
	public void analyze(String[] lines) {
		List<String> names = new ArrayList<>(42);
		List<String> secrets = new ArrayList<>(42);
		List<String> sequences = new ArrayList<>(42);
		
		for (String line : lines) {
			String[] lineSplit = line.split(";");
			names.add(lineSplit[1]);
			secrets.add(lineSplit[2]);
			sequences.add(lineSplit[3]);
		}
		
		long t = System.currentTimeMillis();
		List<Integer> cleartexts = this.decrypt(secrets);
		if ( cleartexts.size() != secrets.size())
			throw new RuntimeException("Not all hashes were cracked!");
		//assert cleartexts.size() == secrets.size() : ;
		System.out.println(cleartexts);
		System.out.println("Decryption: " + (System.currentTimeMillis() - t));

		// t = System.currentTimeMillis();
		// int [] prefixes = this.solve(cleartexts);
		// System.out.println("Linear Combination: " + (System.currentTimeMillis() - t));
			
		t = System.currentTimeMillis();
		int [] partners = this.match(sequences);
		System.out.println("Substring: " + (System.currentTimeMillis() - t));
		Arrays.stream(partners).forEach(System.out::println);
		//System.out.println(partners);
			
		// t = System.currentTimeMillis();
		// List<String> hashes = this.encrypt(partners, prefixes, SerialAnalyzer.prefixLength);
		// System.out.println("Encryption: " + (System.currentTimeMillis() - t));
			
		// for (int i = 0; i < names.size(); i++)
		// 	System.out.println((i + 1) + ";" + names.get(i) + ";" + cleartexts.get(i) + ";" + prefixes[i] + ";" + (partners[i]+ 1) + ";" + hashes.get(i));
	}
	
	private List<Integer> decrypt(List<String> secrets) {
		// int[] cleartexts = new int[secrets.size()];
		// for (int i = 0; i < secrets.size(); i++)
		// 	cleartexts[i] = this.unhash(secrets.get(i));
		
		// secrets.stream()
		int numThreads = 4;
		//might not cover the whole range!!
		int range = SerialAnalyzer.maxPassword/numThreads;
		return IntStream.range(0, numThreads)
			.parallel()
			.mapToObj(index -> {

				List<Integer> tmp = this.unhash(secrets,index*range,range);
				//System.out.println(tmp);
				return tmp;
			})
			.flatMap(lst -> lst.stream())
			.collect(Collectors.toList());
		//return secrets.stream().map(this::unhash).collect(Collectors.toList());
	}
		
	private List<Integer> unhash(List<String> secrets, int start, int range) {
		// int result = IntStream.range(0, SerialAnalyzer.maxPassword)
		// 	.parallel()
		// 	.filter(i-> hash(i).equals(hexHash))
		// 	.findFirst()
		// 	.orElseThrow(()-> new RuntimeException("Cracking failed for " + hexHash));
		// 	System.out.println("Cracked PW: "+ result);
		// return result;
		int end = start+range;
		HashMap<String,Integer> rainbowTable = new HashMap<>();
		for (int i = start; i < end; i++)
		{
			String tmp = this.hash(i);
			rainbowTable.put(tmp, i);
			// if (tmp.equals(hexHash))
			// 	{
			// 		System.out.println("Cracked PW: "+i );
			// 		//return i;
			// 	}
		}
		return secrets
			.stream()
			.filter(x->rainbowTable.containsKey(x))
			.map(x->rainbowTable.get(x))
			.collect(Collectors.toList());
		// if(rainbowTable.containsKey(hexHash))
		// 	return rainbowTable.get(hexHash);
		//throw new RuntimeException("Cracking failed for " + hexHash);
	}
		
	private String hash(int number) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashedBytes = digest.digest(String.valueOf(number).getBytes("UTF-8"));
			
			StringBuffer stringBuffer = new StringBuffer();
			for (int i = 0; i < hashedBytes.length; i++) {
				stringBuffer.append(Integer.toString((hashedBytes[i] & 0xff) + 0x100, 16).substring(1));
			}
			return stringBuffer.toString();
		}
		catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
			throw new RuntimeException(e.getMessage());
		}
	}
	private int ld(long a)
	{
		return (int) (Math.log(a)/Math.log(2));
	}
		
	private int [] solve(List<Integer> numbers) {

		return LongStream.range(0, (long)Math.pow(2, numbers.size()))
			.parallel()
			.mapToObj(a -> {
				String binary = Long.toBinaryString(a);
		    
				int[] prefixes = new int[62];
				for (int i = 0; i < prefixes.length; i++)
					prefixes[i] = 1;
				
				int i = 0;
				for (int j = binary.length()-1; j >= 0; j--) {
					if(binary.charAt(j) == '1')
						prefixes[i] =-1;
					//prefixes[i] = (((a >> j) & 1) == 1 ? -1:1) ;
					i++;
				}
				return prefixes;
			})
			.filter(prefixes -> this.sum(numbers, (int[])prefixes)==0)
			.findFirst().orElseThrow(()-> new RuntimeException("Prefix not found!"));
		// for (long a = 0; a < Long.MAX_VALUE; a++) {
		    

		//     if (this.sum(numbers, prefixes) == 0)
		// 		return prefixes;
		// }
		
		// throw new RuntimeException("Prefix not found!");
	}
	
	private int sum(List<Integer> numbers, int [] prefixes) {
		// int sum = 0;
		// for (int i = 0; i < numbers.length; i++)
		// 	sum += numbers.get(i) * prefixes[i];
		// return sum;
		return IntStream
			.range(0, numbers.size())
			.map(index-> numbers.get(index)*prefixes[index])
			.sum();
	}
		
	private void match(List<String> sequences) {
		int[] partners = new int[sequences.size()];
		for (int i = 0; i < sequences.size(); i++)
			partners[i] = this.longestOverlapPartner(i, sequences);
		return partners;
	}
		
	private int longestOverlapPartner(int thisIndex, List<String> sequences) {
		int bestOtherIndex = -1;
		int bestOverlap = 0;
		for (int otherIndex = 0; otherIndex < sequences.size(); otherIndex++) {
			if (otherIndex == thisIndex)
				continue;
			
			int longestOverlap = LCSubStr(sequences.get(thisIndex), sequences.get(otherIndex));
			 	//this.longestOverlap(sequences.get(thisIndex), sequences.get(otherIndex));

			if (bestOverlap < longestOverlap) {
				bestOverlap = longestOverlap;
				bestOtherIndex = otherIndex;
			}
		}
		return bestOtherIndex;
	}
		
	private String longestOverlap(String str1, String str2) {
        if (str1.isEmpty() || str2.isEmpty()) 
        	return "";
        
        if (str1.length() > str2.length()) {
            String temp = str1;
            str1 = str2;
            str2 = temp;
        }

        int[] currentRow = new int[str1.length()];
        int[] lastRow = str2.length() > 1 ? new int[str1.length()] : null;
        int longestSubstringLength = 0;
        int longestSubstringStart = 0;

        for (int str2Index = 0; str2Index < str2.length(); str2Index++) {
            char str2Char = str2.charAt(str2Index);
            for (int str1Index = 0; str1Index < str1.length(); str1Index++) {
                int newLength;
                if (str1.charAt(str1Index) == str2Char) {
                    newLength = str1Index == 0 || str2Index == 0 ? 1 : lastRow[str1Index - 1] + 1;
                    
                    if (newLength > longestSubstringLength) {
                    	longestSubstringLength = newLength;
                    	longestSubstringStart = str1Index - (newLength - 1);
                    }
                } else {
                    newLength = 0;
                }
                currentRow[str1Index] = newLength;
            }
            int[] temp = currentRow;
            currentRow = lastRow;
            lastRow = temp;
        }
        return str1.substring(longestSubstringStart, longestSubstringStart + longestSubstringLength);
	}
	
	private List<String> encrypt(int[] partners, int[] prefixes, int prefixLength) {
		List<String> hashes = new ArrayList<>(partners.length);
		for (int i = 0; i < partners.length; i++) {
			int partner = partners[i];
			String prefix = (prefixes[i] > 0) ? "1" : "0";
			hashes.add(this.findHash(partner, prefix, prefixLength));
		}
		return hashes;
	}
	
	private String findHash(int content, String prefix, int prefixLength) {
		StringBuilder fullPrefixBuilder = new StringBuilder();
		for (int i = 0; i < prefixLength; i++)
			fullPrefixBuilder.append(prefix);
		
		Random rand = new Random(13);
		
		String fullPrefix = fullPrefixBuilder.toString();
		int nonce = 0;
		while (true) {
			nonce = rand.nextInt();
			String hash = this.hash(content + nonce);
			if (hash.startsWith(fullPrefix))
				return hash;
		}
	}
	private String findHashDet(int content, String prefix, int prefixLength) {
		StringBuilder fullPrefixBuilder = new StringBuilder();
		for (int i = 0; i < prefixLength; i++)
			fullPrefixBuilder.append(prefix);
		
		///Random rand = new Random(13);
		
		String fullPrefix = fullPrefixBuilder.toString();
		int nonce = 0;
		while (true) {
			String hash = this.hash(content + nonce);
			if (hash.startsWith(fullPrefix))
				return hash;
			nonce ++;
		}
	}
}
