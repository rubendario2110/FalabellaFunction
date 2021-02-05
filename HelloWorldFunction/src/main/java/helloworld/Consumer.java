package helloworld;

import javax.persistence.*;

@Entity(name = "falabella")
@Table(name = "response")

public class Consumer {
    private int id;
    private String response;

    @Id
    @Column(name = "idresponse")
    @GeneratedValue
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Column(name = "response")
    public String getResponse() {
        return response;
    }

    public void setResponse(String source) {
        this.response = source;
    }

}
