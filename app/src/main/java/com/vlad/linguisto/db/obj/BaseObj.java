package com.vlad.linguisto.db.obj;

import java.util.logging.Logger;

public abstract class BaseObj {

	protected Integer id;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || !(obj instanceof BaseObj)) return false;
        BaseObj anotherObj = (BaseObj) obj;
        if (id != null ? !id.equals(anotherObj.id) : anotherObj.id != null) return false;
        return true;
    }
}
