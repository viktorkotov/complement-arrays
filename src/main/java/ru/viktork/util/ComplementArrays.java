package ru.viktork.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ComplementArrays
{
    static class Node
    {
    	final int hashCode;
        final String[] arr;

        public Node( String[] arr, int hashCode )
        {
            this.arr = arr;
            this.hashCode = hashCode;
        }

        /*
         * Not immutable
         */
        public String[] getArr() 
        {
			return arr;
		}

		@Override
        public String toString()
        {
            return Arrays.toString( arr );
        }

		@Override
		public int hashCode()
		{
			return hashCode;
		}

		@Override
		public boolean equals( Object obj )
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			
			Node other = (Node) obj;
			return Arrays.equals(arr, other.arr);
		}
    }
	
	protected static int hashCode( String[] arr ) 
	{
		int result = 0;
		for ( int i = 0; i < arr.length; i++ )
		{
		    if ( arr[i] != null )
		    	result += 1 << i;
		}
		return result;
	}
	
	protected static String[] mergeArrays( String[] arrOne, String[] arrTwo ) 
	{
		String[] result = new String[arrOne.length];
		for ( int i = 0; i < arrOne.length; i++ )
		{
			result[i] = arrOne[i] != null 
					? arrOne[i]
					: arrTwo[i];
		}
		return result;
	}
    
    /*
     * TODO check size and large file read as stream by JsonReader 
     */
    public static Map<Integer, Set<Node>> readFile( File file, int[] arrLength ) throws IOException
    {
    	Gson gson = new GsonBuilder().create();
    
    	Map<Integer, Set<Node>> result = new HashMap<>();
    	
        try ( BufferedReader in = new BufferedReader( new FileReader( file ) ) )
        {
        	String[][] nodesTestFrom = gson.fromJson(in, String[][].class);

        	for (String[] arr : nodesTestFrom) 
        	{
        		if( arrLength[0] == -1) arrLength[0] = arr.length;
        		
        		int hashCode = hashCode( arr );
        		result
        		  .computeIfAbsent( hashCode, HashSet::new )
        		  .add( new Node( arr, hashCode ) );
			}
        }
        
        return result;
    }

    /*
     * Необходимо найти все комбинации сумм чисел до 15. Т.е. 1+2+4+8, 1+6+8, 1+2+12 и т.д.
     * Условием совместимости должно быть дополнение до 1 по разрядам в двоичном представлении.
     * Т.е. не должны быть в результате 0001 (1) и 0011 (3), т.к. у них есть совпадение в 1-м разряде.
     */
    protected static Set<Set<Integer>> getAllCombinations( int limit ) 
    {
    	Set<Set<Integer>> result = new HashSet<>();
    	for (int i = 1; i < limit; i++) 
    	{
    		// 1, [2, 4, 6, 8, 10, 12, 14]
    		List<Integer> complements = getComplement(i, limit);
    		
    		Set<Integer> set = new TreeSet<>();
    		set.add( i ); // 1
    		set.add( complements.get( complements.size() - 1 ) ); // 14
    		
    		result.add( set );

    		if( !complements.isEmpty() )
    		{
    			result.addAll( getAllCombinations(i, limit, complements) );
    		}
		}
    	
    	result.add( new HashSet<>( Arrays.asList( limit ) ) );
    	
		return result;
	}
    
    protected static List<Integer> getComplement( int i, int limit )
    {
    	List<Integer> result = new ArrayList<>();
		for (int j = 1; j < limit; j++ ) 
    	{
			if( isComplement(i, j) )
			{
				result.add(j);
			}
    	}

        return result;
    }

    protected static boolean isComplement( int i, int j ) 
    {
		return ( i & j ) == 0;
	}
  
    protected static Set<Set<Integer>> getAllCombinations( final int i, 
    													   final int limit, 
    													   final List<Integer> complements )
	{
		Set<Set<Integer>> result = new HashSet<>();
		
		Queue<Integer> stack = new LinkedList<>();
		int j = 0;
		while( j < complements.size() - 1 )
		{
			stack.add( i );
			int curSum = 0;
			
			while( j < complements.size() - 1 )
			{
				if( curSum == 0 || isComplement( curSum, complements.get(j) ) )
				{
					stack.add( complements.get(j) );
					
					curSum = stack.stream().reduce( 0, (a, b) -> a + b );
					
					if ( curSum > limit )
					{
						stack.poll();
					}
					else if ( curSum == limit )
					{
						result.add( new TreeSet<>( stack ) );
						
						stack.clear();
						break;
					}
				}

				j++;
			}
		}
		
		return result;
	}

	/*
	 * @param setOne source
	 * @param setTwo nodes for change
	 */
    protected static void mergeSets( Set<Node> setOne, 
    								 Set<Node> setTwo )
	{
		if( setTwo.isEmpty() )
		{
			setTwo.addAll( setOne );
		}
		else
		{
			Set<Node> set = new HashSet<>();
			for (Iterator<Node> itOne = setOne.iterator(); itOne.hasNext();) 
			{
				Node nodeOne = itOne.next();
			
				for (Iterator<Node> itTwo = setTwo.iterator(); itTwo.hasNext();) 
				{
					Node nodeTwo = itTwo.next();
	
					String[] merged = mergeArrays( nodeOne.getArr(), nodeTwo.getArr() );
					int hashCode = hashCode( merged );
					
					set.add( new Node( merged, hashCode ) );	
					
					itTwo.remove();
				}
				
				setTwo.addAll( set );
			}
		}
	}

	/*
	 * @param complementHashes complement hashes ([1, 4, 10])
	 * @param allNodes all nodes
	 * @param result results
	 */
    protected static void complement( Set<Integer> complementHashes, 
			                          Map<Integer, Set<Node>> allNodes,
			                          Set<Node> result ) 
	{
		if( !allNodes.keySet().containsAll( complementHashes ) )
		{
			return;
		}
		
        Set<Node> complementNodes = new HashSet<>();
		for (Iterator<Integer> it = complementHashes.iterator(); it.hasNext();) {
			Set<Node> set = allNodes.get( it.next() );
			
			mergeSets( set, complementNodes );
		}
		
		result.addAll( complementNodes );
	}

	public static void main( String[] args ) throws IOException
    {  	
    	if( args.length < 1 ) 
		{
    		System.out.println( "Usage: java ru.viktork.util.ComplementArrays <data_file.json>" );
    		return;
		}
    	
		int[] arrLength = new int[] {-1};
		
		Map<Integer, Set<Node>> nodes = readFile( new File( args[0] ), arrLength  );

        if( nodes.isEmpty() ) return;
        		
		int fullHash = (2 << (arrLength[0]-1));

		Set<Set<Integer>> sets = getAllCombinations( fullHash - 1 );
		
		Set<Node> result = new HashSet<>();
		
		sets.forEach( set -> complement( set, nodes, result ) );
		
		result.forEach(System.out::println);
    }
}