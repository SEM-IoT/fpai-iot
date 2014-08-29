package nl.tno.fpai.driver.plugwise.http;

/**
 * Class for identifying a Plugwise resource; the identification is both an identifier as well as a name.
 */
public class PlugwiseResourceId {
    private final String id;
    private final String name;

    PlugwiseResourceId(String id, String name) {
        this.id = id;
        this.name = name;
    }

    /** @return Identifier of the resource as taken from the $appliance.Id parameter in the Plugwise XML template. */
    public String getId() {
        return id;
    }

    /** @return Name of the resource as taken from the $appliance.Name parameter in the Plugwise XML template. */
    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        PlugwiseResourceId other = (PlugwiseResourceId) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "PlugwiseResourceId [id=" + id + ", name=" + name + "]";
    }
}
