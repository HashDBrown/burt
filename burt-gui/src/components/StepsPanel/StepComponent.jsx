import Modal from "react-modal";
import {ReactComponent as ShowScreenshotStepIcon1} from "../../assets/icons/show_screenshot_step_icon1.svg";
import {ReactComponent as ShowScreenshotStepIcon2} from "../../assets/icons/show_screenshot_step_icon2.svg";
import {ConditionallyRender} from "react-util-kit";
import {ReactComponent as DeleteStepIcon} from "../../assets/icons/delete_step_icon.svg";
import React, {useRef, useState} from "react";
import ApiClient from "../../logic/ApiClient";
import processResponse from "../../logic/ServerResponseProcessor";
import {updateStepsHistory} from "../Chat/chatUtils";
import ContentEditable from "react-contenteditable";
import axios from "axios";
import loadStepHistory from "../../logic/UpdateStepsHistory";
import ImageUtils from "../../logic/ImageUtils";


const customStyles = {
    content: {
        top: '50%',
        left: '50%',
        right: 'auto',
        bottom: 'auto',
        marginRight: '-50%',
        transform: 'translate(-50%, -50%)',
        borderRadius: '5px',
    },
    modalButton: {
        margin: '.2em',
    },
}
Modal.setAppElement('#root');

class StepComponent extends React.Component {

    constructor(props) {
        super(props);

        // console.log("Constructor")
        this.stepNumber = this.props.index + 1
        // For user uploaded file
        this.fileInput = React.createRef();

        this.editableComponentRef = React.createRef();
        this.state = {
            modalIsOpens: Array(this.props.stepsSize).fill(false),
            fullStepDescription: this.props.step.value1,
            currentStepDescription: this.getCroppedDescription(this.props.step.value1),
            stepImage: this.props.step.value2
        }
    }

    setIsOpen(i, v) {
        this.setState(
            {
                modalIsOpens: Object.assign([...this.state.modalIsOpens], {[i]: v})
            }
        );
    };

    //---------------------------

    getCroppedDescription(description) {
        let MAX_LENGTH = 45;
        if (description.length > MAX_LENGTH) {
            description = description.substring(0, MAX_LENGTH) + "..."
        }
        return description;
    }


    //--------------------------------

    openModal(event) {
        if (event.target == this.fileInput.current) {
            // Default behavior is necessary here to open system's file dialog box
            return
        }
        window.onbeforeunload = null;
        event.preventDefault();
        this.setIsOpen(this.props.index, true);
        // this.state.modalIsOpens = Object.assign([...this.state.modalIsOpens], {[this.props.index]: true})
    }

    closeModal(event) {
        event.stopPropagation();
        this.setIsOpen(this.props.index, false);
        // this.state.modalIsOpens = Object.assign([...this.state.modalIsOpens], {[this.props.index]: false})
    }

    //-------------------------------

    deleteLastStep() {

        //disable all prior widget messages/gui components
        let allMsgs = this.props.messagesState.messages
        for (let i = 0; i < allMsgs.length; i++) {
            let msg = allMsgs[i];
            if(msg.widget !== "S2RPredictionConfirmation")
                msg.disabled = true
        }

        let fn = prevState => {
            return {
                ...prevState, messages: allMsgs
            }
        };
        this.props.setStateMsgs(fn)

        //--------------------------

        let message = this.props.actionProvider.createChatBotMessage("Delete step x");
        const responsePromise = ApiClient.processUserMessage(message, [])
        processResponse(responsePromise, this.props.actionProvider, () => {
         /*   let endPoint = this.props.config.serverEndpoint + this.props.config.getStepsHistory
            loadStepHistory(this.props.actionProvider)*/
            loadStepHistory(this.props.actionProvider)
            window.location.reload(false);
        })
    }

    //-----------------------

    handleChange = event => {
        this.setState({currentStepDescription: event.target.value})
    };

    onFocusFn = () => {
        this.setState({currentStepDescription: this.state.fullStepDescription})
    }

    onBlurFn = () => {
        let noBrDescription = this.state.currentStepDescription.replaceAll("<br>", "")
            .replaceAll("&nbsp;", " ").trim()

        if (noBrDescription !== "" && noBrDescription !== this.state.fullStepDescription) {
            this.updateStep(noBrDescription)
        } else {
            this.setState({currentStepDescription: this.getCroppedDescription(this.state.fullStepDescription)})
        }
    }

    handleKeyDown = event => {

        const keyCode = event.which || event.keyCode;
        if (keyCode === 13) { //when the user hits enter
            event.preventDefault();
            this.editableComponentRef.current.blur()
        } else if (keyCode === 27) { //ESC key
            this.editableComponentRef.current.blur()
        }
    };

    updateStep = (newStepDescription) => {
        const endPoint = this.props.config.serverEndpoint + this.props.config.updateStepService;
        const sessionId = this.props.sessionId;

        //------------------------
        const data = {
            sessionId: sessionId,
            messages: [{
                message: newStepDescription,
                selectedValues: [this.props.index]
            }]
        }

        const responsePromise = axios.post(endPoint, data);
        responsePromise.then(response => {

            let result = response.data;
            if (!result) {
                console.error(`The step was not updated: ` + this.props.index);
            } else {
                this.setState({fullStepDescription: newStepDescription})
                this.setState({currentStepDescription: this.getCroppedDescription(this.state.fullStepDescription)})
            }

        }).catch(error => {
            console.error(`There was an unexpected error: ${error}`);
        })

    }

    updateImage = (imageFile) => {
        // Get image update endpoint
        const endPoint = this.props.config.serverEndpoint + this.props.config.updateImageService;

        // Prepare data
        const sessionId = this.props.sessionId;
        const data = {
            sessionId: sessionId,
            messages: [{
                message: this.state.fullStepDescription,
                selectedValues: [this.props.index]
            }]
        }

        const formData = new FormData();
        formData.append("req", new Blob([JSON.stringify(data)], {
            type: "application/json"
        }));
        formData.append("image", imageFile);

        // Send and process post request
        const responsePromise = axios.post(endPoint, formData);
        responsePromise.then(response => {

            let result = response.data;
            if (!result) {
                console.error(`The image was not updated: ` + this.props.index);
            }
        }).catch(error => {
            console.error(`There was an unexpected error: ${error}`);
        })

    }

    onImageInputButtonClick = () => {
        // `current` points to the mounted file input element
        this.fileInput.current.click();
    };

    onImageInputChange = (event) => {
        // If a file was provided
        if (event.target.files[0]) {

            // Update the image in the local state
            const imageUrl = URL.createObjectURL(event.target.files[0]);

            const img = new Image();
            img.src = imageUrl

            ImageUtils.resolutionCalculate(imageUrl,  (resolution) =>  {

                var width = resolution[0]['width']
                var height = resolution[0]['height']
                //Check Key Existence
                if (ImageUtils.resolution_width_height_map.hasOwnProperty(width)) {
                    var height_values = ImageUtils.resolution_width_height_map[width]
                    for (const value of height_values) {
                        // If image height is a valid resolution
                        if (height === value) {
                            // Update the image on the server
                            this.updateImage(event.target.files[0]);

                            this.setState({stepImage: imageUrl});

                            // Update the image in the parent components states
                            // Have to recreate the stepState object because it won't trigger the hook unless its updated with a new reference
                            let tempSteps = {};
                            tempSteps.steps = this.props.stepsState.steps;
                            tempSteps.steps[this.props.index].value2 = imageUrl;
                            this.props.setStepsState(tempSteps)

                            this.updateStep(this.state.fullStepDescription);
                        } else {
                            alert("Change height of the image to one of the values =>"+ ImageUtils.toString())
                        }
                    }
                } else {
                    alert("Change resolution of the image to one of the values =>" + ImageUtils.toString())
                }
            })


        }
    }

    //------------------------

    render() {
        return (
            <div className="list-group-item">
                <span>{this.stepNumber + ". "}</span>
                <ContentEditable
                    innerRef={this.editableComponentRef}
                    tagName="span"
                    title={this.state.fullStepDescription}
                    html={this.state.currentStepDescription} // innerHTML of the editable div
                    onChange={this.handleChange} // handle innerHTML change
                    onKeyDown={this.handleKeyDown}
                    onFocus={this.onFocusFn}
                    onBlur={this.onBlurFn}
                />
                <a href={this.state.stepImage} title={"See a screenshot of this step"}
                   onClick={event => this.openModal(event)}>
                    <Modal
                        isOpen={this.state.modalIsOpens[this.props.index]}
                        onRequestClose={event => this.closeModal(event)}
                        style={customStyles}
                        contentLabel="Example Modal"
                        onHide={event => this.closeModal(event)}
                        backdrop="static"
                        keyboard={false}>
                        <div className="popup-display">
                            <div className="popup-title"
                                 title={this.state.fullStepDescription}>{this.state.fullStepDescription}</div>
                            <img height="533px" width="300px" src={this.state.stepImage}
                                 alt={this.state.fullStepDescription}/>
                            <div>
                                <button onClick={this.onImageInputButtonClick} style={customStyles.modalButton}>Upload Image</button>
                                <input type='file' id='imageFileInp' ref={this.fileInput}  onChange={this.onImageInputChange} accept="image/*" style={{display: 'none'}}/>
                                <button onClick={event => this.closeModal(event)} style={customStyles.modalButton}>Close</button>
                            </div>
                        </div>
                    </Modal>

                    <span className="label label-left label-info">
                    <ShowScreenshotStepIcon1 className="bi bi-file-earmark-image"/>
                    <ShowScreenshotStepIcon2 className="bi bi-zoom-in"/>
                 </span>
                </a>

                <ConditionallyRender
                    ifTrue={this.props.isLastStep}
                    show={
                        <a href="#" className="label label-danger" onClick={() => this.deleteLastStep()}
                           title="Delete this step">
                            <DeleteStepIcon className="bi bi-x-circle"/>
                        </a>
                    }
                />
            </div>
        );
    }


}


export default StepComponent;