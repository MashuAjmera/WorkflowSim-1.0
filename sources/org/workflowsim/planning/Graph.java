

package org.workflowsim.planning;

import java.util.*;

class Graph {
  private int V;

  public int count;

  private LinkedList<Integer> adj[];

  @SuppressWarnings("unchecked")
  Graph(int v) {
    V = v;
    adj = new LinkedList[v];
    for (int i = 0; i < v; ++i)
      adj[i] = new LinkedList();
  }

  void addEdge(int v, int w) {
    adj[v].add(w);
  }

  void DFSUtil(int v, boolean visited[]) {

    visited[v] = true;
    count++;

    Iterator<Integer> i = adj[v].listIterator();
    while (i.hasNext()) {
      int n = i.next();
      if (!visited[n])
        DFSUtil(n, visited);
    }
  }

  int DFS(int v) {

    boolean visited[] = new boolean[V];

    DFSUtil(v, visited);
    int m= --count;
    		count=0;
    return m;
  }
  
  public ArrayList<Integer> childs(int v)    {
	    ArrayList<Integer> numbers = new ArrayList<Integer>();
	    Iterator<Integer> i = adj[v].listIterator();
	    while (i.hasNext()) {
	        int n = i.next();
	        numbers.add(n);
	      }
	    
	    
	    return numbers;
	}
  public ArrayList<Integer> bpath(int v,Graph g)    {
	  
      ArrayList<Integer> best_path = new ArrayList<Integer>();
      best_path.add(0);
      
      ArrayList<Integer> m,n2 = new ArrayList<Integer>();
	   m=g.childs(0);
      while (true) {
   	  
   	   
   	  
   	int m1=0,m2=-1;
   	Iterator<Integer> i = m.listIterator();
  	    while (i.hasNext()) {
  	        int n = i.next();
  	        int t=g.childs(n).size();
  	        if (t>=m1) {
  	        	m1=t;
  	        	m2=n;
  	      }
  	     best_path.add(n);
  	     m=g.childs(n);
  	        
  	       
  	        
  	    }
  	 if (m1==0) {
       	break;
       }
  	 
  	 
      }
      return best_path;
  }
 
  void removeEdge(int v) {
	    for (int i = 0; i < V; i++) {
	      for (int j = 0; j < adj[i].size(); j++) {
	        if (adj[i].get(j) == v) {
	          adj[i].remove(j);
	          break;
	        }
	      }
	    }
	    while (adj[v].size() > 0) {
	      adj[v].remove();
	    }
	  }
  
}

