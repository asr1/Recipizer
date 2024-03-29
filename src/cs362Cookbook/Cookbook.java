package cs362Cookbook;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

import Interfaces.Category_I;
import Interfaces.Cookbook_I;
import Interfaces.Database_Support_I;
import Interfaces.Ingredient_I;
import Interfaces.Recipe_I;



public class Cookbook implements Cookbook_I
{
	
	public Database_Support_I db;
	
	private int editID;
	
	public Cookbook() {
		db = new Database_Support();
		editID = -1;
	}

	public boolean removeCategory(String name, int ID)
	{
		Recipe r = (Recipe) db.getRecipe(ID);
		Category cat = (Category) db.getCategory(name);
		if(r == null || cat == null)
		{
			return false;
		}
		 r.removeCategory(cat);
		 cat.removeRecipe(r);
		 return db.putRecipe(r) >0 && db.putCategory(cat);
		 
	}

	@Override
	public boolean rate(int ID, Rating rating)
	{
		Recipe r = (Recipe) db.getRecipe(ID);
		if(r == null)
		{
			return false;
		}
		r.rate(rating);
		return db.putRecipe(r) > 0;
	}
	
	@Override
	public boolean unrate(int ID)
	{
		Recipe r = (Recipe) db.getRecipe(ID);
		if(r == null)
		{
			return false;
		}
		r.unrate();
		return db.putRecipe(r) > 0;
	}
	
	/**
	 * Takes a name and adds that ingredient to the cookbook. 
	 * Returns a boolean whether it was successful or not.
	 * 
	 * @param name
	 * @return boolean
	 */
	@Override
	public boolean addIngredient(String name) {
		
		Ingredient_I I = new Ingredient(name);
		
		return db.putIngredient(I);		
	}

	@Override
	public boolean discardRecipe()
	{
		editID = -1;
		File file = new File("temp.txt");
		return file.delete();
	}

	/**
	 * Takes a ID number for a recipe and makes a duplicate of that recipe. 
	 * Returns the ID of the newly created recipe.
	 * 
	 * @param ID
	 * @return int
	 */
	@Override
	public boolean duplicateRecipe(int ID) {
		
		Recipe_I R1 = db.getRecipe(ID);
		
		if(R1 == null) 
		{
			return false;
		}
		
		Recipe_I R2 = R1.copyRecipe();
		
		return db.putRecipe(R2) > 0;
		
	}

	//returns true if succeeded, otherwise false
	@Override
	public boolean editRecipe(int ID)
	{
		if(editID == -1)
		{
			//save editted recie's ID
			editID = ID;
			
			//Get Recipe from database
			Recipe recipe = (Recipe) db.getRecipe(ID);
			
			if(recipe == null)
			{
				return false;
			}
			
			//print recipe data to new text file temp.txt
			File file = new File("temp.txt");
			PrintWriter writer = null;
			try {
				writer = new PrintWriter(file);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}
			
			//Print Name
			writer.println("#Name");
			writer.println(recipe.name);
			
			//Print Authors
			writer.println("#Authors");
			writer.println(recipe.author);
			
			//Print Instructions
			writer.println("#Instruction");
			writer.println(recipe.instruction);

			//Print ingredients
			writer.println("#Ingredients");
			List<Ingredient_I> ingredients = recipe.getIngredients(db);
			for(int i = 0; i < ingredients.size(); i++)
			{
				writer.println(((Ingredient)ingredients.get(i)).getName());
			}
			writer.close();
			//prompt changes
			System.out.println("temp.txt created, please edit the file to make changes");
			System.out.println("Save chabges to recipe? <Y/N>");
			
			//process input
			Scanner scan = new Scanner(System.in);
			String input = "";
			while (scan.hasNext())
			{
				input = scan.nextLine();
				if(input.equals("Y")||input.equals("N")){
					break;
				}
			}
			scan.close();
			
			//save edits to recipe
			if(input.equals("Y"))
			{
				saveRecipe();
			}
			
			//
			else{
				discardRecipe();
			}
		}
		else
		{
			System.out.println("Please finish editting: "+editID);	
		}
		return false;
	}

	private void editsWait(Scanner edits, String input){
		//wait for next non-#starting line
		while(edits.hasNext()&&(input.charAt(0)==('#')))
		{
			input = edits.nextLine();
		}
	}
	
	@Override
	public boolean removeIngredient(String name)
	{
		Ingredient ing = (Ingredient) db.getIngredient(name);

		if(ing == null)
		{
			return false;
		}
		
		return db.deleteIngredient(ing);
	}

	@Override
	public boolean removeRecipe(int ID)
	{
		Recipe r = (Recipe) db.getRecipe(ID);

		if(r == null)
		{
			return false;
		}
		
		return db.deleteRecipe(r);
	}

	/**
	 * Takes a old ingredient and a new ingredient then uses the new one in place of the old one. 
	 * Returns a boolean whether it was successful or not.
	 * 
	 * @param oName, nName
	 * @return boolean
	 */
	@Override
	public boolean replaceIngredient(String oName, String nName) {
		
		Ingredient_I I_old = db.getIngredient(oName);
		Ingredient_I I_new = db.getIngredient(nName);
		
		if(I_old == null || I_new == null) {
			return false;
		}
		
		List<Recipe_I> list = I_old.getRecipes(db);
		
		for(Recipe_I R : list) {
			R.addIngredient(I_new);
			R.removeIngredient(I_old);
			db.putRecipe(R);
		}
		
		db.putIngredient(I_new);
		
		return db.deleteIngredient(I_old);
		
	}

	public int getIngredient(String name){
		return db.getIngredient(name).getID();
	}
	
	@Override
	public boolean saveRecipe()
	{
		Recipe recipe = (Recipe) db.getRecipe(editID);

		if(recipe == null)
		{
			return false;
		}
		
		String input = "";
		File file = new File("temp.txt");
		
		//process inputs
		Scanner edits = null;
		try {
			edits = new Scanner(file);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		//wait for first non-#starting line
		if(edits.hasNextLine()){
			input=edits.nextLine();
		}
		editsWait(edits, input);
		
		//clear recipe name then set equal to next non-#starting lines 
		recipe.name="";
		while(edits.hasNext()&&!(input.charAt(0)==('#')))
		{
			recipe.name+=edits.nextLine()+" ";
		}
		editsWait(edits, input);			
		
		//clear recipe author then set equal to next non-#starting lines 
		recipe.author="";
		while(edits.hasNext()&&!(input.charAt(0)==('#')))
		{
			recipe.author+=input+" ";
			input = edits.nextLine();
		}
		editsWait(edits, input);
		
		//clear recipe instructions then set equal to next non-#starting lines 
		recipe.instruction="";
		while(edits.hasNext()&&!(input.charAt(0)==('#')))
		{
			recipe.instruction+=input+" ";
			input = edits.nextLine();
		}
		editsWait(edits, input);
		
		//clear recipe ingredients then set equal to next non-#starting lines 
		recipe.ingredients.clear();
		while(edits.hasNext()&&!(input.charAt(0)==('#')))
		{
			recipe.instruction+=input+" ";
			input = edits.nextLine();
		}
		return db.putRecipe(recipe) > 0;
	}

	@Override
	public boolean addRecipe(String name, String author, List<Integer> ingredients, String instruction)
	{
		Recipe recipe = new Recipe(name, author, ingredients, instruction);
		return db.putRecipe(recipe) > 0;
	}

	@Override
	public boolean show(int ID)
	{
		Recipe rec = (Recipe) db.getRecipe(ID);
		if(rec == null)
		{
			return false;
		}
		rec.show();
		return true;

	}

	/**
	 * Takes a name and adds that category to the cookbook. 
	 * Returns a boolean whether it was successful or not.
	 * 
	 * @param name
	 * @return boolean
	 */
	@Override
	public boolean addCategory(String name) {
		Category_I C = new Category(name);
		return db.putCategory(C);
	}

	/**
	 * Hides the given recipe (via ID) from the cookbook.
	 * Returns a boolean whether it was successful or not. 
	 * 
	 * @param ID
	 * @return boolean
	 */
	@Override
	public boolean hideRecipe(int ID) {
		Recipe_I R = db.getRecipe(ID);

		if(R == null)
		{
			return false;
		}
		
		R.hide();
		return db.putRecipe(R) > 0;
	}

	/**
	 * Exports the given recipe (via ID) to a file for sharing.
	 * Returns a String with the shared file's name.
	 * 
	 * @param ID
	 * @return String
	 */
	@Override
	public String share(int ID) {
		Recipe_I R = db.getRecipe(ID);		
		return R.export(db);
	}

	@Override
	public boolean favoriteRecipe(int ID) {
		Recipe_I R = db.getRecipe(ID);

		if(R == null)
		{
			return false;
		}
		
		R.favorite();
		return db.putRecipe(R) > 0;
	}

	@Override
	public boolean unfavoriteRecipe(int ID) {
		Recipe_I R = db.getRecipe(ID);

		if(R == null)
		{
			return false;
		}
		
		R.unfavorite();
		return db.putRecipe(R) > 0;
	}

	/**
	 * Does a search of all the recipes.
	 * Returns all the recipes in the system.
	 * 
	 * @return List<Recipe_I>
	 */
	@Override
	public List<Recipe_I> search() {
		return db.getAllRecipes();
	}
	
	/**
	 * Sorts the given recipes by their categories.
	 * Returns the sorted list of recipes.
	 * 
	 * @param List<Recipe_I>
	 * @return List<Recipe_I>
	 */
	@Override
	public List<Recipe_I> sortCategory(List<Recipe_I> L) {
		
		// bubble sort cause I'm cool
		for(int i = 0; i < L.size(); i++) {
			
			Recipe_I R1 = L.get(i);
			List<Category_I> categories1 = R1.getCategories(db);
			Category_I C1 = important(categories1);
			
			// sort level
			for(int j = i + 1; j < L.size(); j++) {
				
				Recipe_I R2 = L.get(j);
				List<Category_I> categories2 = R2.getCategories(db);
				Category_I C2 = important(categories2);
				
				if(!C1.compare(C2)) {
					
					// swap the two things
					L.set(i, R2);
					L.set(j, R1);
					R1 = R2;
					
				}
				
			}
		}
		
		return L;
	}
	
	/**
	 * Helper function for sortCategory
	 * Returns the most important Category for comparison
	 * 
	 * @param List<Category_I>
	 * @return Category_I
	 */
	private Category_I important(List<Category_I> L) {
		Category_I C = null;
		
		// get the most important Category for comparing.
		for(Category_I c : L) {
			if(!C.compare(c)) {
				C = c;
			}
		}
		
		return C;
	}

	@Override
	public List<Recipe_I> sortRating(List<Recipe_I> L)
	{
		//Not using Bubble sort because I'm cooler
		Collections.sort(L, new Comparator<Recipe_I>()
		{
			  @Override
			  public int compare(Recipe_I x, Recipe_I y) {
			    return x.getRating().compareTo(y.getRating());
			  }
		});
		return L;
	}

	@Override
	public List<Recipe_I> sortAlphabetic(List<Recipe_I> L) {
		Collections.sort(L, new Comparator<Recipe_I>()
		{
			  @Override
			  public int compare(Recipe_I x, Recipe_I y) {
			    return x.getName().compareTo(y.getName());
			  }
		});
		return L;
	}

	@Override
	public List<Recipe_I> sortAuthor(List<Recipe_I> L) {
		Collections.sort(L, new Comparator<Recipe_I>()
		{
			  @Override
			  public int compare(Recipe_I x, Recipe_I y) {
			    return x.getAuthor().compareTo(y.getAuthor());
			  }
		});
		return L;
	}

	@Override
	public List<Recipe_I> filterSource(String source)
	{
		List<Recipe_I> recs = db.getAllRecipes();
		List<Recipe_I> ret = new ArrayList<Recipe_I>();
		for(Recipe_I rec : recs)
		{
			if(rec.getAuthor().equals(source))
			{
				ret.add(rec);
			}
		}
		
		return ret;
	}

	@Override
	public List<Recipe_I> filterIngredient(String ingredient)
	{
		Ingredient_I ing = db.getIngredient(ingredient);		
		return ing.getRecipes(db);
	}

	//Wouldn't it be better to get that category from the database
	//And then return all of the recipes that that category knows?
	//that's what I thought with ingredient^^
	@Override
	public List<Recipe_I> filterCategory(String category)
	{
		List<Recipe_I> recs = db.getAllRecipes();
		List<Recipe_I> ret = new ArrayList<Recipe_I>();
		
		for(Recipe_I rec : recs)
		{
			List<Category_I> cats = new ArrayList<Category_I>();
			for(Category_I cat : cats)
			{
				if(cat.getName().equals(category))
				{
					ret.add(rec);
					continue;
				}
			}
		}
		
		return ret;
	}

	@Override
	public void printRecipe(Recipe_I result) {
		System.out.println("Recipe:"+result.getName());
		System.out.println("by: "+result.getAuthor());
		System.out.println("Ingredients:");
		for(Ingredient_I ingredient : result.getIngredients(db)){
			System.out.println(ingredient.getName());
		}
		System.out.println("Categories:");
		for(Category_I category: result.getCategories(db)){
			System.out.println(category.getName());
		}
		System.out.println("Instructions: "+result.getInstruction());
		System.out.println();
	}
}


