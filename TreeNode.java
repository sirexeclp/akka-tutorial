import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class TreeNode<T> //implements Iterable<TreeNode<T>>
{

	T data;
	int level;
    TreeNode<T> parent;
//	HashMap<T,TreeNode<T>> leafs;
	List<TreeNode<T>> leafs;
	HashMap<T,TreeNode<T>> children;
	TreeNode<T> root;

    public TreeNode(T data) {
        this.data = data;
		this.children = new HashMap<>();
		this.leafs = new ArrayList<>();//HashMap<>();
		this.level = 0;
		this.root = this;
	}
	public void printPath()
	{
		TreeNode<T> iter = this;
		while(iter != root)
		{
			System.out.print(iter.data +" | ");
			iter = iter.parent;

		}
		System.out.println("");
	}
	@Override
	public String toString() {
		return data.toString()+"lvl"+level;
	}
	public List<TreeNode<T>> findLeafs()
	{
		List<TreeNode<T>> leafs = new ArrayList<>();
		for (TreeNode<T> childNode : children.values()) {
			if(childNode.isLeaf())
				leafs.add(childNode);
			else
				leafs.addAll(childNode.findLeafs());
		}
		return leafs;
	}
	public boolean isLeaf()
	{
		return this.children.isEmpty();
	}

    public TreeNode<T> addChild(T child) {
		TreeNode<T> childNode;
		if(this.children.containsKey(child))
			childNode = this.children.get(child);
		else
		{
			childNode = new TreeNode<T>(child);
			childNode.parent = this;
			childNode.level = this.level+1;
			childNode.root = this.root;
			this.children.put(child,childNode);
			// if(this.root.leafs.containsKey(childNode.data))
			// {
			// 	if(this.root.leafs.get(childNode.data).level<childNode.level)
			// 		this.root.leafs.replace(childNode.data, childNode);
			// }
			// else
			// 	this.root.leafs.put(childNode.data,childNode);
			this.root.leafs.add(childNode);
			this.root.leafs.remove(this);
			//this.root.leafs.remove(this.data);
		}
        return childNode;
	}
	public void print()
	{
		System.out.println(this.data);
		System.out.println("|");
		for (TreeNode<T> childNode : children.values()) {
			System.out.print(childNode.data);
			System.out.print(" ");
		}
		System.out.println("");
		for (TreeNode<T> childNode : children.values()) {
			childNode.print();
		}
	}
	public static void main(String [] args){

		List<String> strings = new ArrayList<>();
		strings.add("ABAB");
		strings.add("BABA");
		strings.add("ABBA");
		TreeNode<String> root = new TreeNode<>("");
		for(int i =0; i < strings.size(); i++) {
			String item = strings.get(i) + "$" +i;
			System.out.println(item);
			TreeNode<String> current = root;
			List<TreeNode<String>> previous = new ArrayList<>();
			for(int j = 0; j < item.length(); j++)
			{
				Character c = item.toCharArray()[j];
				previous.add(current);
				boolean endOfString = false;
				for (int k = previous.size()-1; k >= 0; k--) {
					TreeNode node = previous.get(k);
					if(c == '$')
					{
						node.addChild(""+c+item.toCharArray()[j+1]);
						endOfString = true;
					}
					//previous.remove(node);
					TreeNode<String> tmp = node.addChild(c.toString());
					if (previous != tmp)
						previous.add(tmp);
				}
				if(endOfString)
					j++;
			}
		}
		System.out.println("so weit so gut");
		root.print();

		List<TreeNode<String>> leafs = root.findLeafs()
			.stream()
			.filter(x-> x.data.equals("$0") || x.data.equals("$1"))
			.sorted((a,b)-> a.level<b.level?-1:1)
			.collect(Collectors.toList());
		System.out.println(leafs);
		System.out.println(root.leafs);
		TreeNode<String> a=null,b=null;

		for (TreeNode x : root.leafs) {
			if(x.data.equals("$0"))
				a = x;
		}
		for (TreeNode x : root.leafs) {
			if(x.data.equals("$1")&&x.level == a.level)
				b = x;
		}

		TreeNode<String> parent = null;
		a.printPath();
		b.printPath();
		while(parent == null)
		{
			if(a.parent == b.parent)
				parent = a.parent;
			else
			{
				System.out.println(a.parent);
				System.out.println(b.parent);
				if(a.level>b.level)
					a = a.parent;
				else
					b = b.parent;
			}
		}
		System.out.println("OK");
		
		parent.printPath();
		//System.out.println(LCSubStr("BABA", "ABAB"));

	}
	static String LCSubStr(String a, String b)  
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
		
        return a.substring(end-result+1,end+1);
    } 

}