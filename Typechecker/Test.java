class Main {
    public static void main(String[] s){
        int x;
        int[] arr;
        boolean b;
        Parent dafad;
        Parent p;
        Child c;
        Child newBorn;
        Car honda;
        Car mustang;
        p = c.getparent(x);
        x = c.getint();
        p = new Child();
        x = 3-2;
        x = x*x;
        x = x-0;
        arr = new int[3];
        arr[2] = 4;
        arr[3] = x;
        arr[x] = 2;
        arr[3] = 3+3;
        System.out.println(1);
        b = b;
        b = true;
        b = b && b;
        b = b && true;
        if ((3+4) < 5) {
            arr[arr[1]] = 3;
        } else {
            b = !b;
            arr[arr.length] = arr.length;
        }
        while (!!b) {
            b = b;
        }
        newBorn = honda.crash(mustang);       
    }
}


class Parent {
    int x;
    public Parent getparent(int z) {
        Child a;
        return this;
    }
    public int getint() {
        return x;
    }
}

class Child extends Parent {
    int y;
    boolean dead;
    public Child born() {
        dead = false;
        y = 0;
        return this;
    }
    public Child die() {
        dead = true;
        return this;
    }
	
	public int callObscureMethod(int x) {
		int y;
		boolean z;
		Hatchback h;
		x = h.obscureMethod(x);
		//x = h.obscureMethod(x, y);
		x = h.obscureMethod(z);
		return x;
	}
}

class A extends B {}

class Car {
    Child passenger;
    public Child crash(Car crashInto) {
        passenger = new Child().die();
        return passenger;
    }
}

class Truck extends Car {
    int numWheels;
    Parent driver;
    public int setWheels(int n) {
        numWheels = n;
        return numWheels;
    }
}

class Hatchback extends Car {
	int x;
	int y;
	boolean z;
	public int obscureMethod(int y) {
		y = 3;
		x = y;
		z = (x<y) && z;
		return x;
	}
}

class OldCar extends Car{
    
}

class ModifiedCar extends Car{}


