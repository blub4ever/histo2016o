package org.histo.model.util;

/**
 * Alle Objekte werden nicht gel�scht, sondern nurch archiviert.
 * @author andi
 *
 */
public interface ArchiveAble {
  
    public boolean isArchived();
    
    /**
     * Setzt das Objekt und alles Kinder als "archived"
     * @param archived
     */
    public void setArchived(boolean archived);
    
    /**
     * Gibt den Namen des Objektes zur�ck
     * @return
     */
    public String getTextIdentifier();
    
    /**
     * Gibt den Dilaog zum Archivieren zur�ck
     * @return
     */
    public String getArchiveDialog();
}
