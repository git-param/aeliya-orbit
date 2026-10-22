# Permissions & Organization Structure

## Core Concepts
- **Roles:** System access levels (Admin, HR, Manager, Employee).
- **Designations:** Job titles.
- **Departments:** Functional groups.
- **Reporting:** Hierarchical manager-employee relationships.

## Unresolved Access Decisions (TODO)
- Can Managers view payroll details of their reports?
- Who can override/approve attendance anomalies?
- Do HR roles have implicit read access to all complaints?

## Department permission catalog
- Department permission cards show the count of allowed catalog entries. Editing a card loads its existing grants as checked boxes; Save replaces that department's selections and Cancel discards the draft.
- Each entry in the `modules` catalog has a `section` used to group the editor. Existing entries belong to Employee. Dashboard and Configuration sections appear with an empty state until their permissions are specified and added to the catalog. Future sections come from catalog data rather than additional page layouts.
