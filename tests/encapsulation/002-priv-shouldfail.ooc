
class Dog {

	private String name
	Int age

	func new

	func setName(=name)
	func getName name

}

func main {

	dog = new Dog
	dog name = "Dogbert" // ERROR
	dog age = 18
	printf("I've a dog named %s and aged %d\n", dog name, dog age)

}