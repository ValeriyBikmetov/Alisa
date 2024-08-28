package valeriy.bikmetov.alisa.model;


/**  Класс представляет элемент списка (выбранное значение списка)
 *  id - идентификатор элемента списка (id - автора, серии, жанра или книги)
 *  name - текстовое значение списка (имя автора, название серии ...)
 * @author Валерий
 */
public class Item implements Comparable<Item>{
    private int id;
    private String name;

    public Item(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int compareTo(Item o) {
        return this.name.compareTo(o.name);
    }
}