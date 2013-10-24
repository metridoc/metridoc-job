package entity
import javax.persistence.Entity
import javax.persistence.Id

@Entity
class Bar {
    @Id
    Long id
    String name
}