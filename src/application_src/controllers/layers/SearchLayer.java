/*
 * Bao Lab 2017
 */

package application_src.controllers.layers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TreeItem;
import javafx.scene.paint.Color;

import application_src.application_model.logic.lineage.LineageData;
import application_src.application_model.internal_data.connectome.Connectome;
import application_src.application_model.internal_data.partslist.PartsList;
import application_src.application_model.logic.search.SearchType;
import application_src.application_model.logic.search.SearchUtil;
import application_src.application_model.internal_data.anatomy.AnatomyTerm;
import application_src.application_model.logic.cell_case.CasesLists;
import application_src.application_model.logic.color_rule.Rule;
import application_src.application_model.logic.color_rule.SearchOption;
import application_src.application_model.threeD.subscenegeometry.SceneElementsList;
import application_src.application_model.threeD.subscenegeometry.StructureTreeNode;
import application_src.application_model.ProductionInfo;
import application_src.application_model.logic.search.GeneSearchService;

import static java.lang.Thread.sleep;
import static java.util.Arrays.asList;
import static java.util.Collections.sort;
import static java.util.Objects.requireNonNull;

import static javafx.application.Platform.runLater;
import static javafx.scene.paint.Color.DARKSEAGREEN;
import static javafx.scene.paint.Color.web;

import static application_src.application_model.internal_data.partslist.PartsList.getFunctionalNameByLineageName;
import static application_src.application_model.internal_data.partslist.PartsList.getLineageNamesByFunctionalName;
import static application_src.application_model.internal_data.partslist.PartsList.isLineageName;
import static application_src.application_model.logic.search.SearchType.CONNECTOME;
import static application_src.application_model.logic.search.SearchType.DESCRIPTION;
import static application_src.application_model.logic.search.SearchType.FUNCTIONAL;
import static application_src.application_model.logic.search.SearchType.GENE;
import static application_src.application_model.logic.search.SearchType.LINEAGE;
import static application_src.application_model.logic.search.SearchType.MULTICELLULAR_STRUCTURE_CELLS;
import static application_src.application_model.logic.search.SearchType.STRUCTURES_BY_HEADING;
import static application_src.application_model.logic.search.SearchType.STRUCTURE_BY_SCENE_NAME;
import static application_src.application_model.logic.search.SearchUtil.getAncestorsList;
import static application_src.application_model.logic.search.SearchUtil.getCellBodiesList;
import static application_src.application_model.logic.search.SearchUtil.getCellsInMulticellularStructure;
import static application_src.application_model.logic.search.SearchUtil.getCellsWithConnectivity;
import static application_src.application_model.logic.search.SearchUtil.getCellsWithFunctionalDescription;
import static application_src.application_model.logic.search.SearchUtil.getCellsWithFunctionalName;
import static application_src.application_model.logic.search.SearchUtil.getCellsWithLineageName;
import static application_src.application_model.logic.search.SearchUtil.getDescendantsList;
import static application_src.application_model.logic.search.SearchUtil.getNeighboringCells;
import static application_src.application_model.logic.search.SearchUtil.isGeneFormat;
import static application_src.application_model.logic.search.WormBaseQuery.issueWormBaseQuery;
import static application_src.application_model.logic.lineage.LineageTree.getCaseSensitiveName;
import static application_src.application_model.internal_data.anatomy.AnatomyTerm.AMPHID_SENSILLA;
import static application_src.application_model.logic.color_rule.SearchOption.ANCESTOR;
import static application_src.application_model.logic.color_rule.SearchOption.CELL_BODY;
import static application_src.application_model.logic.color_rule.SearchOption.CELL_NUCLEUS;
import static application_src.application_model.logic.color_rule.SearchOption.DESCENDANT;

public class SearchLayer {

    private final Service<Void> resultsUpdateService;
    private final GeneSearchService geneSearchService;
    private final Service<Void> showLoadingService;

    private final ObservableList<Rule> rulesList;

    private final ObservableList<String> searchResultsList;

    // gui components
    private final TextField searchTextField;
    private final ToggleGroup searchTypeToggleGroup;
    private final CheckBox presynapticCheckBox;
    private final CheckBox postsynapticCheckBox;
    private final CheckBox neuromuscularCheckBox;
    private final CheckBox electricalCheckBox;
    private final CheckBox cellNucleusCheckBox;
    private final CheckBox cellBodyCheckBox;
    private final CheckBox ancestorCheckBox;
    private final CheckBox descendantCheckBox;
    private final ColorPicker colorPicker;
    private final Button addRuleButton;

    /** Tells the subscene controller to rebuild the 3D subscene according to the fetched gene results */
    private final BooleanProperty geneResultsUpdatedFlag;
    /** Tells the subscene controller to rebuild the 3D subscene */
    private final BooleanProperty rebuildSubsceneFlag;

    // queried databases
    private Connectome connectome;
    private CasesLists casesLists;
    private ProductionInfo productionInfo;
    private WiringService wiringService;
    private TreeItem<StructureTreeNode> structureTreeRoot;

    public SearchLayer(
            final ObservableList<Rule> rulesList,
            final ObservableList<String> searchResultsList,
            final TextField searchTextField,
            final RadioButton systematicRadioButton,
            final RadioButton functionalRadioButton,
            final RadioButton descriptionRadioButton,
            final RadioButton geneRadioButton,
            final RadioButton connectomeRadioButton,
            final RadioButton multicellRadioButton,
            final Label descendantLabel,
            final CheckBox presynapticCheckBox,
            final CheckBox postsynapticCheckBox,
            final CheckBox neuromuscularCheckBox,
            final CheckBox electricalCheckBox,
            final CheckBox cellNucleusCheckBox,
            final CheckBox cellBodyCheckBox,
            final CheckBox ancestorCheckBox,
            final CheckBox descendantCheckBox,
            final ColorPicker colorPicker,
            final Button addRuleButton,
            final BooleanProperty geneResultsUpdatedFlag,
            final BooleanProperty rebuildSubsceneFlag) {

        this.rulesList = requireNonNull(rulesList);
        this.searchResultsList = requireNonNull(searchResultsList);

        // text field
        this.searchTextField = requireNonNull(searchTextField);
        this.searchTextField.textProperty().addListener(getTextFieldListener());

        // search options
        final ChangeListener<Boolean> optionsCheckBoxListener = getOptionsCheckBoxListener();
        this.cellNucleusCheckBox = requireNonNull(cellNucleusCheckBox);
        this.cellNucleusCheckBox.selectedProperty().addListener(optionsCheckBoxListener);
        this.cellBodyCheckBox = requireNonNull(cellBodyCheckBox);
        this.cellBodyCheckBox.selectedProperty().addListener(optionsCheckBoxListener);
        this.ancestorCheckBox = requireNonNull(ancestorCheckBox);
        this.ancestorCheckBox.selectedProperty().addListener(optionsCheckBoxListener);
        this.descendantCheckBox = requireNonNull(descendantCheckBox);
        this.descendantCheckBox.selectedProperty().addListener(optionsCheckBoxListener);

        final ChangeListener<Boolean> connectomeCheckBoxListener = getConnectomeCheckBoxListener();
        this.presynapticCheckBox = requireNonNull(presynapticCheckBox);
        this.presynapticCheckBox.selectedProperty().addListener(connectomeCheckBoxListener);
        this.postsynapticCheckBox = requireNonNull(postsynapticCheckBox);
        this.postsynapticCheckBox.selectedProperty().addListener(connectomeCheckBoxListener);
        this.neuromuscularCheckBox = requireNonNull(neuromuscularCheckBox);
        this.neuromuscularCheckBox.selectedProperty().addListener(connectomeCheckBoxListener);
        this.electricalCheckBox = requireNonNull(electricalCheckBox);
        this.electricalCheckBox.selectedProperty().addListener(connectomeCheckBoxListener);

        // color
        this.colorPicker = requireNonNull(colorPicker);

        // add rule button
        this.addRuleButton = requireNonNull(addRuleButton);
        this.addRuleButton.setOnAction(getAddButtonClickHandler());

        this.resultsUpdateService = new Service<Void>() {
            @Override
            protected final Task<Void> createTask() {
                final Task<Void> task = new Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        runLater(() -> refreshSearchResultsList(
                                (SearchType) searchTypeToggleGroup.getSelectedToggle().getUserData(),
                                getSearchedText(),
                                cellNucleusCheckBox.isSelected(),
                                cellBodyCheckBox.isSelected(),
                                descendantCheckBox.isSelected(),
                                ancestorCheckBox.isSelected()));
                        return null;
                    }
                };
                return task;
            }
        };

        this.rebuildSubsceneFlag = requireNonNull(rebuildSubsceneFlag);
        this.geneResultsUpdatedFlag = requireNonNull(geneResultsUpdatedFlag);

        showLoadingService = new ShowLoadingService();

        geneSearchService = new GeneSearchService();
        geneSearchService.setOnScheduled(event -> showLoadingService.restart());
        geneSearchService.setOnCancelled(event -> {
            showLoadingService.cancel();
            searchResultsList.clear();
            geneSearchService.resetSearchedGene();
        });

        geneSearchService.setOnSucceeded(event -> {
            showLoadingService.cancel();
            searchResultsList.clear();

            final String searchedGene = geneSearchService.getSearchedGene();
            updateGeneResults(searchedGene);

            // set the cells for gene-based rules if not already set
            final String searchedQuoted = "'" + searchedGene + "'";
            rulesList.stream()
                    .filter(rule -> rule.isGeneRule()
                            && !rule.areCellsSet()
                            && rule.getSearchedText().contains(searchedQuoted))
                    .forEach(rule -> rule.setCells(geneSearchService.getValue()));
        });

        // search type toggle
        searchTypeToggleGroup = new ToggleGroup();
        initSearchTypeToggleGroup(
                requireNonNull(systematicRadioButton),
                requireNonNull(functionalRadioButton),
                requireNonNull(descriptionRadioButton),
                requireNonNull(geneRadioButton),
                requireNonNull(connectomeRadioButton),
                requireNonNull(multicellRadioButton),
                requireNonNull(descendantLabel));
    }

    private void initSearchTypeToggleGroup(
            final RadioButton systematicRadioButton,
            final RadioButton functionalRadioButton,
            final RadioButton descriptionRadioButton,
            final RadioButton geneRadioButton,
            final RadioButton connectomeRadioButton,
            final RadioButton multicellRadioButton,
            final Label descendantLabel) {

        systematicRadioButton.setToggleGroup(searchTypeToggleGroup);
        systematicRadioButton.setUserData(LINEAGE);

        functionalRadioButton.setToggleGroup(searchTypeToggleGroup);
        functionalRadioButton.setUserData(FUNCTIONAL);

        descriptionRadioButton.setToggleGroup(searchTypeToggleGroup);
        descriptionRadioButton.setUserData(DESCRIPTION);

        geneRadioButton.setToggleGroup(searchTypeToggleGroup);
        geneRadioButton.setUserData(GENE);

        connectomeRadioButton.setToggleGroup(searchTypeToggleGroup);
        connectomeRadioButton.setUserData(CONNECTOME);

        multicellRadioButton.setToggleGroup(searchTypeToggleGroup);
        multicellRadioButton.setUserData(MULTICELLULAR_STRUCTURE_CELLS);

        searchTypeToggleGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            // if toggle was previously on 'gene' then cancel whatever wormbase search was issued
            if (oldValue != null && oldValue.getUserData() == GENE) {
                geneSearchService.cancel();
                searchResultsList.clear();
            }
            final SearchType type = (SearchType) newValue.getUserData();
            // disable descendant options for terminal cell searches
            if (type == FUNCTIONAL || type == DESCRIPTION) {
                descendantCheckBox.setSelected(false);
                descendantCheckBox.disableProperty().set(true);
                descendantLabel.disableProperty().set(true);
            } else {
                descendantCheckBox.disableProperty().set(false);
                descendantLabel.disableProperty().set(false);
            }
            // re-search whatever is in the search field with this new search type
            resultsUpdateService.restart();
        });

        // select lineage search on start
        systematicRadioButton.setSelected(true);
    }

    public void setStructureTreeRoot(final TreeItem<StructureTreeNode> root) {
        structureTreeRoot = requireNonNull(root);
    }

    /**
     * Adds a giant connectome rule that contains all the cell results retrieved based on the input query parameters
     *
     * @param funcName
     *         the functional name of the cell
     * @param color
     *         color to apply to cell entities
     * @param isPresynapticTicked
     *         true if the presynaptic option was ticked, false otherwise
     * @param isPostsynapticTicked
     *         true if the postsynaptic option was ticked, false otherwise
     * @param isElectricalTicked
     *         true if the electrical option was ticked, false otherwise
     * @param isNeuromuscularTicked
     *         true if the neuromuscular option was ticked, false otherwise
     *
     * @return the rule that was added to the internal list
     */
    public Rule addConnectomeColorRuleFromContextMenu(
            final String funcName,
            final Color color,
            final boolean isPresynapticTicked,
            final boolean isPostsynapticTicked,
            final boolean isElectricalTicked,
            final boolean isNeuromuscularTicked) {

        final StringBuilder sb = createLabelForConnectomeRule(
                funcName,
                isPresynapticTicked, isPostsynapticTicked, isElectricalTicked, isNeuromuscularTicked);
        final Rule rule = new Rule(rebuildSubsceneFlag, sb.toString(), color, CONNECTOME, CELL_NUCLEUS);
        rule.setCells(connectome.queryConnectivity(
                funcName,
                isPresynapticTicked,
                isPostsynapticTicked,
                isElectricalTicked,
                isNeuromuscularTicked,
                true));
        rule.setSearchedText(sb.toString());
        rule.resetLabel(sb.toString());
        rulesList.add(rule);
        return rule;
    }

    private StringBuilder createLabelForConnectomeRule(
            String funcName,
            final boolean isPresynapticTicked,
            final boolean isPostsynapticTicked,
            final boolean isElectricalTicked,
            final boolean isNeuromuscularTicked) {

        final StringBuilder sb = new StringBuilder("'");
        sb.append(funcName.toLowerCase()).append("' Connectome");

        final List<String> types = new ArrayList<>();
        if (isPresynapticTicked) {
            types.add("presynaptic");
        }
        if (isPostsynapticTicked) {
            types.add("postsynaptic");
        }
        if (isElectricalTicked) {
            types.add("electrical");
        }
        if (isNeuromuscularTicked) {
            types.add("neuromuscular");
        }
        if (!types.isEmpty()) {
            sb.append(" - ");

            for (int i = 0; i < types.size(); i++) {
                sb.append(types.get(i));
                if (i != types.size() - 1) {
                    sb.append(", ");
                }
            }
        }

        return sb;
    }

    private void updateGeneResults(final String searchedGene) {
        final List<String> results = geneSearchService.getPreviouslyFetchedGeneResults(searchedGene);
        if (results == null || results.isEmpty()) {
            return;
        }

        if (descendantCheckBox.isSelected()) {
            getDescendantsList(results, searchedGene)
                    .stream()
                    .filter(name -> !results.contains(name))
                    .forEachOrdered(results::add);
        }
        if (ancestorCheckBox.isSelected()) {
            getAncestorsList(results, searchedGene)
                    .stream()
                    .filter(name -> !results.contains(name))
                    .forEachOrdered(results::add);
        }
        if (!cellNucleusCheckBox.isSelected()) {
            final Iterator<String> iterator = results.iterator();
            while (iterator.hasNext()) {
                if (iterator.next().equalsIgnoreCase(searchedGene)) {
                    iterator.remove();
                    break;
                }
            }
        }
        sort(results);
        appendFunctionalToLineageNames(results);
        geneResultsUpdatedFlag.set(true);
    }

    private void appendFunctionalToLineageNames(final List<String> list) {
        searchResultsList.clear();
        for (String result : list) {
            if (getFunctionalNameByLineageName(result) != null) {
                result += " (" + getFunctionalNameByLineageName(result) + ")";
            }
            searchResultsList.add(result);
        }
    }

    private String getSearchedText() {
        final String searched = searchTextField.getText().toLowerCase();
        return searched;
    }

    /**
     * Adds the app's internal color rules. These rules are used when the active story does not have its own color
     * scheme.
     */
    public void addDefaultInternalColorRules() {
        addColorRule(FUNCTIONAL, "ash", DARKSEAGREEN, CELL_BODY);
        addColorRule(FUNCTIONAL, "rib", web("0x663366"), CELL_BODY);
        addColorRule(FUNCTIONAL, "avg", web("0xb41919"), CELL_BODY);

        addColorRule(FUNCTIONAL, "dd", web("0x4a24c1", 0.60), CELL_BODY);
        addColorRule(FUNCTIONAL, "da", web("0xc56002"), CELL_BODY);

        addColorRule(FUNCTIONAL, "rivl", web("0xff9966"), CELL_BODY);
        addColorRule(FUNCTIONAL, "rivr", web("0xffe6b4"), CELL_BODY);
        addColorRule(FUNCTIONAL, "sibd", web("0xe6ccff"), CELL_BODY);
        addColorRule(FUNCTIONAL, "siav", web("0x99b3ff"), CELL_BODY);

        addColorRule(FUNCTIONAL, "dd1", web("0xb30a95"), CELL_NUCLEUS);
        addColorRule(FUNCTIONAL, "dd2", web("0xb30a95"), CELL_NUCLEUS);
        addColorRule(FUNCTIONAL, "dd3", web("0xb30a95"), CELL_NUCLEUS);
        addColorRule(FUNCTIONAL, "dd4", web("0xb30a95"), CELL_NUCLEUS);
        addColorRule(FUNCTIONAL, "dd5", web("0xb30a95"), CELL_NUCLEUS);
        addColorRule(FUNCTIONAL, "dd6", web("0xb30a95"), CELL_NUCLEUS);

        addColorRule(FUNCTIONAL, "da2", web("0xe6b34d"), CELL_NUCLEUS);
        addColorRule(FUNCTIONAL, "da3", web("0xe6b34d"), CELL_NUCLEUS);
        addColorRule(FUNCTIONAL, "da4", web("0xe6b34d"), CELL_NUCLEUS);
        addColorRule(FUNCTIONAL, "da5", web("0xe6b34d"), CELL_NUCLEUS);

        addStructureRuleBySceneName("lim4_bundle_left", web("0xe6ccff"));
        addStructureRuleBySceneName("lim4_bundle_left", web("0x99b3ff"));
        addStructureRuleBySceneName("lim4_bundle_right", web("0xe6ccff"));
        addStructureRuleBySceneName("lim4_bundle_right", web("0x99b3ff"));
        addStructureRuleBySceneName("lim4_nerve_ring", web("0xff9966"));
        addStructureRuleBySceneName("lim4_nerve_ring", web("0xffe6b4"));
        addStructureRuleBySceneName("Amphid Commissure Right", DARKSEAGREEN);
        addStructureRuleBySceneName("Amphid Commissure Right", web("0x663366"));
        addStructureRuleBySceneName("Amphid Commissure Left", DARKSEAGREEN);
        addStructureRuleBySceneName("Amphid Commissure Left", web("0x663366"));
    }

    /**
     * Adds a color rule for a multicellular structure to the currently active rules list. Adding a rule does not
     * rebuild the subscene. In order for any changes to be visible, the calling class must set the
     * 'rebuildSubsceneFlag' to true or set a property that triggers a subscene rebuild.
     *
     * @param searched
     *         the searched structure
     * @param color
     *         the color to apply to the structure
     *
     * @return the multicellular structure rule added
     */
    public Rule addStructureRuleBySceneName(final String searched, final Color color) {
        return addColorRule(STRUCTURE_BY_SCENE_NAME, searched, color, new ArrayList<>());
    }

    /**
     * Adds a color rule for a collection of multicellular structures under a heading in the structures tree in the
     * Find Structures tab. All the structures under sub-headings are affected by the rule as well. Adding a rule does
     * not rebuild the subscene. In order for any changes to be visible, the calling class must set the
     * 'rebuildSubsceneFlag' to true or set a property that triggers a subscene rebuild.
     *
     * @param heading
     *         the structures heading
     * @param color
     *         the color to apply to all structures under the heading
     *
     * @return the color rule, null if there was no heading
     */
    public Rule addStructureRuleByHeading(final String heading, final Color color) {
        final Rule rule = addColorRule(STRUCTURES_BY_HEADING, heading, color, new ArrayList<>());

        final List<String> structuresToAdd = new ArrayList<>();
        final Queue<TreeItem<StructureTreeNode>> nodeQueue = new LinkedList<>();
        nodeQueue.add(structureTreeRoot);

        // find the node with the desired heading
        TreeItem<StructureTreeNode> headingNode = null;

        TreeItem<StructureTreeNode> treeItem;
        StructureTreeNode node;
        while (!nodeQueue.isEmpty()) {
            treeItem = nodeQueue.remove();
            if (treeItem != null) {
                node = treeItem.getValue();
                if (node.isHeading()) {
                    if (node.getNodeText().equalsIgnoreCase(heading)) {
                        headingNode = treeItem;
                        break;
                    } else {
                        nodeQueue.addAll(treeItem.getChildren());
                    }
                }
            }
        }

        // get all structures under this heading (structures in sub-headings are included as well)
        if (headingNode != null) {
            nodeQueue.clear();
            nodeQueue.add(headingNode);
            while (!nodeQueue.isEmpty()) {
                treeItem = nodeQueue.remove();
                node = treeItem.getValue();
                if (node.isHeading()) {
                    nodeQueue.addAll(treeItem.getChildren());
                } else {
                    structuresToAdd.add(node.getSceneName());
                }
            }
            rule.setCells(structuresToAdd);
        }
        return rule;
    }

    /**
     * Adds a color rule to the currently active rules list. Adding a rule does not rebuild the subscene. In order
     * for any changes to be visible, the calling class must set the 'rebuildSubsceneFlag' to true or set a property
     * that triggers a subscene rebuild.
     *
     * @param searchType
     *         the search type
     * @param searched
     *         the searched term
     * @param color
     *         the color to apply to the cells in the search results
     * @param options
     *         the search options
     *
     * @return the rule added to the active rules list
     */
    public Rule addColorRule(
            final SearchType searchType,
            String searched,
            final Color color,
            final SearchOption... options) {
        return addColorRule(searchType, searched, color, new ArrayList<>(asList(options)));
    }

    /**
     * Adds a color rule to the currently active rules list. Adding a rule does not rebuild the subscene. In order
     * for any changes to be visible, the calling class must set the 'rebuildSubsceneFlag' to true or set a property
     * that triggers a subscene rebuild.
     *
     * @param searchType
     *         the search type
     * @param searched
     *         the searched term
     * @param color
     *         the color to apply to the cells in the search results
     * @param options
     *         the search options
     *
     * @return the rule added to the active rules list
     */
    public Rule addColorRule(
            final SearchType searchType,
            final String searched,
            final Color color,
            List<SearchOption> options) {

        // default search options is cell
        if (options == null) {
            options = new ArrayList<>();
            options.add(CELL_NUCLEUS);
        }

        final Rule rule = new Rule(
                rebuildSubsceneFlag,
                createRuleLabel(searched, searchType),
                color,
                searchType,
                options);
        rule.setCells(getCellsList(searchType, searched));
        rulesList.add(rule);
        searchResultsList.clear();
        return rule;
    }

    /**
     * Adds a color rule to the currently active rules list, specified only by URL.
     * This is not a searchable rule. It is a Manually Specified List (MSL) that can
     * only be defined in URL format. See documentation in code_README
     *
     * @param names
     * @param color
     * @param options
     * @return
     */
    public Rule addColorRule(
            final List<String> names,
            final Color color,
            final List<SearchOption> options) {

        //
        final Rule rule = new Rule(
                rebuildSubsceneFlag,
                createRuleLabel(names),
                color,
                SearchType.MSL,
                options
        );
        rule.setCells(getCellsList(names));
        rulesList.add(rule);
        return rule;

    }

    private String createRuleLabel(List<String> names) {
        StringBuilder labelBuilder = new StringBuilder();
        labelBuilder.append("'");
        for (String name : names) {
            labelBuilder.append(name);
            labelBuilder.append("; ");
        }
        // remove last two characters after last name
        labelBuilder.deleteCharAt(labelBuilder.length()-1);
        labelBuilder.deleteCharAt(labelBuilder.length()-1);

        labelBuilder.append("' ").append(SearchType.MSL.toString());
        return labelBuilder.toString();
    }

    private String createRuleLabel(String searched, final SearchType searchType) {
        searched = searched.trim();
        StringBuilder labelBuilder = new StringBuilder();
        if (searchType != null) {
            if (searchType == LINEAGE) {
                labelBuilder.append(getCaseSensitiveName(searched));
                if (labelBuilder.toString().isEmpty()) {
                    labelBuilder.append(searched);
                }
            } else if (searchType == CONNECTOME) {
                labelBuilder = createLabelForConnectomeRule(
                        searched,
                        presynapticCheckBox.isSelected(),
                        postsynapticCheckBox.isSelected(),
                        neuromuscularCheckBox.isSelected(),
                        electricalCheckBox.isSelected());
            } else {
                labelBuilder.append("'").append(searched).append("' ").append(searchType.toString());
            }
        } else {
            labelBuilder.append(searched);
        }
        return labelBuilder.toString();
    }

    private List<String> getCellsList(final List<String> names) {
        List<String> lineageNames = new ArrayList<String>();

        for (String name : names) {
            if (SearchUtil.isLineageName(name)) { // lineage name already
                lineageNames.add(name);
            } else if (SearchUtil.isMulticellularStructureByName(name)) { // get all the cells associated with structure
                List<String> cells = getCellsInMulticellularStructure(name);
                for (String cell : cells) {
                    lineageNames.add(cell);
                }
            } else { // functional name
                List<String> cells = PartsList.getLineageNamesByFunctionalName(name);
                for (String cell : cells) {
                    lineageNames.add(cell);
                }
            }
        }

        return lineageNames;
    }

    private List<String> getCellsList(final SearchType type, final String searched) {
        List<String> cells = new ArrayList<>();
        if (type != null) {
            switch (type) {
                case LINEAGE:
                    cells = getCellsWithLineageName(searched);
                    break;

                case FUNCTIONAL:
                    cells = getCellsWithFunctionalName(searched);
                    break;

                case DESCRIPTION:
                    cells = getCellsWithFunctionalDescription(searched);
                    break;

                case GENE:
                    switch (geneSearchService.getState()) {
                        case RUNNING:
                            geneSearchService.cancel();
                        case CANCELLED:
                        case SUCCEEDED:
                            geneSearchService.reset();
                            geneSearchService.resetSearchedGene();
                            break;
                    }
                    if (isGeneFormat(searched)) {
                        final List<String> geneCells = geneSearchService.getPreviouslyFetchedGeneResults(searched);
                        if (geneCells != null) {
                            return geneCells;
                        } else {
                            geneSearchService.setSearchedGene(searched);
                            geneSearchService.start();
                        }
                    }
                    break;

                case MULTICELLULAR_STRUCTURE_CELLS:
                    cells = getCellsInMulticellularStructure(searched);
                    break;

                case CONNECTOME:
                    cells = getCellsWithConnectivity(
                            searched,
                            presynapticCheckBox.isSelected(),
                            postsynapticCheckBox.isSelected(),
                            neuromuscularCheckBox.isSelected(),
                            electricalCheckBox.isSelected());
                    break;

                case NEIGHBOR:
                    cells = getNeighboringCells(searched);
            }
        }

        return cells;
    }

    public Rule addGeneColorRuleFromUrl(final String searched, final Color color, final SearchOption... options) {
        return addGeneColorRuleFromUrl(searched, color, new ArrayList<>(asList(options)));
    }

    public Rule addGeneColorRuleFromUrl(final String searched, final Color color, List<SearchOption> options) {
        if (options == null) {
            options = new ArrayList<>();
            options.add(CELL_NUCLEUS);
        }
        final String label = createRuleLabel(searched, GENE);
        final Rule rule = new Rule(rebuildSubsceneFlag, searched, color, GENE, options);
        final List<String> cells = geneSearchService.getPreviouslyFetchedGeneResults(searched);
        if (cells != null) {
            rule.setCells(cells);
        } else {
            final Service<List<String>> queryService = new Service<List<String>>() {
                public Task<List<String>> createTask() {
                    return new Task<List<String>>() {
                        public List<String> call() {
                            return issueWormBaseQuery(searched);
                        }
                    };
                }
            };
            queryService.setOnSucceeded(event -> {
                final List<String> results = queryService.getValue();
                rule.setCells(results);
                rebuildSubsceneFlag.set(true);
                geneSearchService.cacheGeneResults(searched, results);
            });
            queryService.start();
        }
        rulesList.add(rule);
        return rule;
    }

    public EventHandler<ActionEvent> getAddButtonClickHandler() {
        return event -> {
            // do not add new ColorRule if search has no matches
            if (searchResultsList.isEmpty()) {
                return;
            }

            final List<SearchOption> options = new ArrayList<>();
            if (cellNucleusCheckBox.isSelected()) {
                options.add(CELL_NUCLEUS);
            }
            if (cellBodyCheckBox.isSelected()) {
                options.add(CELL_BODY);
            }
            if (ancestorCheckBox.isSelected()) {
                options.add(ANCESTOR);
            }
            if (descendantCheckBox.isSelected()) {
                options.add(DESCENDANT);
            }

            addColorRule(
                    (SearchType) searchTypeToggleGroup.getSelectedToggle().getUserData(),
                    getSearchedText(),
                    colorPicker.getValue(),
                    options);

            searchTextField.clear();
        };
    }

    public ChangeListener<Boolean> getOptionsCheckBoxListener() {
        return (observableValue, oldValud, newValue) -> {
            if (searchTypeToggleGroup.getSelectedToggle().getUserData() == GENE) {
                updateGeneResults(getSearchedText());
            } else {
                resultsUpdateService.restart();
            }
        };
    }

    public ObservableList<String> getSearchResultsList() {
        return searchResultsList;
    }

    private ChangeListener<String> getTextFieldListener() {
        return (observable, oldValue, newValue) -> {
            if (searchTextField.getText().isEmpty()) {
                searchResultsList.clear();
            } else {
                resultsUpdateService.restart();
            }
        };
    }



    private void refreshSearchResultsList(
            final SearchType searchType,
            String searchedTerm,
            final boolean isCellNucleusFetched,
            final boolean isCellBodyFetched,
            final boolean areDescendantsFetched,
            final boolean areAncestorsFetched) {

        if (!searchedTerm.isEmpty()) {
            searchedTerm = searchedTerm.trim().toLowerCase();

            final List<String> cells = getCellsList(searchType, searchedTerm);

            if (cells != null) {
                final String searchedText = getSearchedText();
                final List<String> cellsForListView = new ArrayList<>();
                if (areDescendantsFetched) {
                    getDescendantsList(cells, searchedText)
                            .stream()
                            .filter(name -> !cellsForListView.contains(name))
                            .forEach(cellsForListView::add);
                }
                if (areAncestorsFetched) {
                    getAncestorsList(cells, searchedText)
                            .stream()
                            .filter(name -> !cellsForListView.contains(name))
                            .forEach(cellsForListView::add);
                }
                if (isCellNucleusFetched) {
                    cellsForListView.addAll(cells);
                } else if (isCellBodyFetched) {
                    cellsForListView.addAll(getCellBodiesList(cells));
                }

                sort(cellsForListView);
                appendFunctionalToLineageNames(cellsForListView);
            }
        }
    }

    /**
     * Sets the databases that are queried during a search
     *
     * @param lineageData
     *         the lineage data
     * @param sceneElementsList
     *         the list of scene elements
     * @param connectome
     *         the connectome
     * @param casesList
     *         the list of cell cases
     * @param productionInfo
     *         the production info
     */
    public void initDatabases(
            final LineageData lineageData,
            final SceneElementsList sceneElementsList,
            final Connectome connectome,
            final CasesLists casesList,
            final ProductionInfo productionInfo) {

        SearchUtil.initDatabases(lineageData, sceneElementsList, connectome, casesList);

        if (connectome != null) {
            this.connectome = connectome;
        }
        if (casesList != null) {
            this.casesLists = casesList;
        }
        if (productionInfo != null) {
            this.productionInfo = productionInfo;
        }
    }

    public boolean hasCellCase(final String cellName) {
        return casesLists != null && casesLists.hasCellCase(cellName);
    }

    public void removeCellCase(final String cellName) {
        if (casesLists != null && cellName != null) {
            casesLists.removeCellCase(cellName);
        }
    }

    public void addToInfoWindow(final AnatomyTerm term) {
        if (term.equals(AMPHID_SENSILLA)) {
            if (!casesLists.containsAnatomyTermCase(term.getTerm())) {
                casesLists.makeAnatomyTermCase(term);
            }
        }
    }

    /**
     * Method taken from RootLayoutController --> how can InfoWindowLinkController generate page without pointer to
     * RootLayoutController?
     */
    public void addToInfoWindow(final String name) {
        if (wiringService == null) {
            wiringService = new WiringService();
        }
        wiringService.setSearchString(name);
        wiringService.restart();
    }

    private ChangeListener<Boolean> getConnectomeCheckBoxListener() {
        return (observable, oldValue, newValue) -> resultsUpdateService.restart();
    }

    public Service<Void> getResultsUpdateService() {
        return resultsUpdateService;
    }

    private final class WiringService extends Service<Void> {

        private String searchString;

        public String getSearchString() {
            final String searched = searchString;
            return searched;
        }

        public void setSearchString(final String searchString) {
            this.searchString = requireNonNull(searchString);
        }

        @Override
        protected Task<Void> createTask() {
            return new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    List<String> searchedCells = new ArrayList<>();
                    String searched = getSearchString();
                    // update to lineage name if functional
                    final List<String> lineageNames = getLineageNamesByFunctionalName(searched);
                    if (!lineageNames.isEmpty()) {
                        searchedCells.addAll(lineageNames);
                    } else {
                        searchedCells.add(searched);
                    }

                    for (String searchedCell : searchedCells) {
                        // GENERATE CELL TAB ON CLICK
                        if (searchedCell != null && !searchedCell.isEmpty()) {
                            if (casesLists == null || productionInfo == null) {
                                return null; // error check
                            }
                            if (isLineageName(searchedCell)) {
                                if (casesLists.containsCellCase(searchedCell)) {
                                    // show the tab
                                } else {
                                    // translate the name if necessary
                                    String funcName = connectome.checkQueryCell(searchedCell).toUpperCase();
                                    // add a terminal case --> pass the wiring partners
                                    casesLists.makeTerminalCase(
                                            searchedCell,
                                            funcName,
                                            connectome.queryConnectivity(
                                                    funcName,
                                                    true,
                                                    false,
                                                    false,
                                                    false,
                                                    false),
                                            connectome.queryConnectivity(
                                                    funcName,
                                                    false,
                                                    true,
                                                    false,
                                                    false,
                                                    false),
                                            connectome.queryConnectivity(
                                                    funcName,
                                                    false,
                                                    false,
                                                    true,
                                                    false,
                                                    false),
                                            connectome.queryConnectivity(
                                                    funcName,
                                                    false,
                                                    false,
                                                    false,
                                                    true,
                                                    false),
                                            productionInfo.getNuclearInfo(),
                                            productionInfo.getCellShapeData(searchedCell));
                                }
                            } else {
                                // not in connectome --> non terminal case
                                if (casesLists.containsCellCase(searchedCell)) {
                                    // show tab
                                } else {
                                    // add a non terminal case
                                    casesLists.makeNonTerminalCase(
                                            searchedCell,
                                            productionInfo.getNuclearInfo(),
                                            productionInfo.getCellShapeData(searchedCell));
                                }
                            }
                        }
                    }
                    return null;
                }
            };
        }
    }

    /**
     * Service that shows when gene results are being fetched by the {@link GeneSearchService} so that the user does
     * not think that the application is not responding.
     */
    private final class ShowLoadingService extends Service<Void> {

        /** Time between changes in the number of ellipses periods during loading */
        private static final long WAIT_TIME_MILLIS = 1000;

        /** Maximum number of ellipses periods to show, plus 1 */
        private static final int MODULUS = 5;

        /** Changing number of ellipses periods to display during loading */
        private int count = 0;

        @Override
        protected final Task<Void> createTask() {
            return new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    while (true) {
                        if (isCancelled()) {
                            break;
                        }
                        runLater(() -> {
                            String loadingString = "Fetching data from WormBase";
                            int num = count % MODULUS;
                            for (int i = 0; i < num; i++) {
                                loadingString += ".";
                            }
                            searchResultsList.clear();
                            searchResultsList.add(loadingString);
                        });
                        try {
                            sleep(WAIT_TIME_MILLIS);
                            count++;
                            count %= MODULUS;
                        } catch (InterruptedException ie) {
                            break;
                        }
                    }
                    return null;
                }
            };
        }
    }
}