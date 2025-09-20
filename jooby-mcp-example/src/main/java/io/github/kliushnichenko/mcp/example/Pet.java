package io.github.kliushnichenko.mcp.example;

/**
 * Example Pet class to demonstrate complex object schema generation.
 */
public class Pet {
    private String name;
    private int age;
    private boolean vaccinated;

    public Pet() {}

    public Pet(String name, int age, boolean vaccinated) {
        this.name = name;
        this.age = age;
        this.vaccinated = vaccinated;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public boolean isVaccinated() {
        return vaccinated;
    }

    public void setVaccinated(boolean vaccinated) {
        this.vaccinated = vaccinated;
    }

    @Override
    public String toString() {
        return "Pet{" +
               "name='" + name + '\'' +
               ", age=" + age +
               ", vaccinated=" + vaccinated +
               '}';
    }
}